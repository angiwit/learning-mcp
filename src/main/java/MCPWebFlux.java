import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.List;
import java.util.Random;

public class MCPWebFlux {
    private static final int PORT = 8182;

    private static final String MESSAGE_ENDPOINT = "/sse/message";

    public static void main(String[] args) {

        WebFluxSseServerTransportProvider mcpServerTransportProvider = new WebFluxSseServerTransportProvider(new ObjectMapper(), MESSAGE_ENDPOINT);

        HttpHandler httpHandler = RouterFunctions.toHttpHandler(mcpServerTransportProvider.getRouterFunction());
        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);
        DisposableServer httpServer = HttpServer.create().port(PORT).handle(adapter).bindNow();
        System.out.println("http server started");

        McpServerFeatures.AsyncToolSpecification tool = getToolSpecification();

        McpServer
                .async(mcpServerTransportProvider)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)         // Enable tool support
                        .logging()           // Enable logging support
                        .build())
                .serverInfo("test-server", "1.0.0")
                .tools(tool).build();
        httpServer.onDispose().block();
    }

    private static McpServerFeatures.AsyncToolSpecification getToolSpecification() {
        var schema = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "operation" : {
                  "type" : "string"
                },
                "a" : {
                  "type" : "number"
                },
                "b" : {
                  "type" : "number"
                }
              }
            }
            """;
        return new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool("calculator", "Basic calculator", schema),
                (exchange, arguments) -> {
                    // Tool implementation
                    return Mono.just(new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("result:" + new Random().nextInt(1000000))), false));
                }
        );
    }


}
