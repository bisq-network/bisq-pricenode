/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.spot;

import com.sun.net.httpserver.HttpServer;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * Test support for XChange-based {@link ExchangeRateProvider}s. Serves recorded API
 * responses of an exchange from a local HTTP server and points the real XChange client
 * of the provider at it, so {@code doGet()} runs the full XChange initialization and
 * response parsing with no network access.
 *
 * Unlike {@link MockedExchangeSupport}, which mocks the XChange client away, this
 * catches an exchange response which the XChange version in use cannot parse.
 */
public final class RecordedExchangeSupport implements AutoCloseable {

    private final HttpServer server;
    private final Set<String> unexpectedPaths = ConcurrentHashMap.newKeySet();
    private final AtomicReference<RuntimeException> responseFailure = new AtomicReference<>();

    /**
     * @param responsesByPath response body for each request path, computed from the
     *                        request URI; any other path is answered with 404
     */
    public RecordedExchangeSupport(Map<String, Function<URI, byte[]>> responsesByPath) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", httpExchange -> {
            try (httpExchange) {
                URI uri = httpExchange.getRequestURI();
                Function<URI, byte[]> response = responsesByPath.get(uri.getPath());
                if (response == null) {
                    unexpectedPaths.add(uri.getPath());
                    httpExchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] body;
                try {
                    body = response.apply(uri);
                } catch (RuntimeException e) {
                    responseFailure.compareAndSet(null, e);
                    httpExchange.sendResponseHeaders(500, -1);
                    return;
                }
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                httpExchange.sendResponseHeaders(200, body.length);
                try (OutputStream outputStream = httpExchange.getResponseBody()) {
                    outputStream.write(body);
                }
            }
        });
        server.start();
    }

    /**
     * @return the recorded response in the given classpath resource, whatever the query
     */
    public static Function<URI, byte[]> resource(String name) {
        byte[] body = readResource(name);
        return uri -> body;
    }

    public static byte[] readResource(String name) {
        try (InputStream inputStream = RecordedExchangeSupport.class.getClassLoader().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing test resource " + name);
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Runs {@code doGet()} of the provider with its XChange client sending all requests
     * to this server. Fails if the client requested a path without a recorded response,
     * if computing a response failed, if creating the exchange failed or if no rate was
     * retrieved, because {@code doGet()} itself only logs a failure and returns no rates.
     *
     * @return price of each retrieved exchange rate by currency; two rates for one
     * currency fail with an {@link IllegalStateException}
     */
    public Map<String, Double> doGet(ExchangeRateProvider provider) {
        AtomicReference<RuntimeException> creationFailure = new AtomicReference<>();
        ExchangeRateProvider spy = spy(provider);
        doAnswer(invocation -> {
            try {
                return createExchange(invocation.getArgument(0));
            } catch (RuntimeException e) {
                creationFailure.set(e);
                throw e;
            }
        }).when(spy).createExchange(any());

        Set<ExchangeRate> rates = spy.doGet();

        if (!unexpectedPaths.isEmpty()) {
            throw new AssertionError("No recorded response for request paths " + unexpectedPaths);
        }
        if (responseFailure.get() != null) {
            throw new AssertionError("Computing a recorded response failed", responseFailure.get());
        }
        if (creationFailure.get() != null) {
            throw new AssertionError("Creating the exchange failed", creationFailure.get());
        }
        if (rates.isEmpty()) {
            throw new AssertionError("No rates retrieved, see the log of the provider for the cause");
        }
        return rates.stream()
                .collect(Collectors.toMap(ExchangeRate::getCurrency, ExchangeRate::getPrice));
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private Exchange createExchange(Class<? extends Exchange> exchangeClass) {
        ExchangeSpecification specification = ExchangeFactory.INSTANCE
                .createExchangeWithoutSpecification(exchangeClass)
                .getDefaultExchangeSpecification();
        int port = server.getAddress().getPort();
        specification.setSslUri("http://127.0.0.1:" + port + "/");
        specification.setHost("127.0.0.1");
        specification.setPort(port);
        return ExchangeFactory.INSTANCE.createExchange(specification);
    }
}
