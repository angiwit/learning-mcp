import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@Log4j2
public class Main {
    public static void main(String[] args1) throws InterruptedException, LifecycleException {
        McpServerTransportProvider transportProvider = new HttpServletSseServerTransportProvider(new ObjectMapper(), "/sse/message");
        // Create an async server with custom configuration
        McpAsyncServer asyncServer = McpServer.async(transportProvider)
                .serverInfo("my-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)         // Enable tool support
                        .logging()           // Enable logging support
                        .build())
                .build();

        asyncServer.addTool(getToolSpecifiction())
                .doOnSuccess(v -> log.info("Tool registered"))
                .subscribe();

        asyncServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("custom-logger")
                .data("Server initialized")
                .build());
        System.out.println("server initialized");
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        Context ctx = tomcat.addContext("", null);
        Tomcat.addServlet(ctx, "hello", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.getWriter().println("Hello, REST!");
            }
        });
        Tomcat.addServlet(ctx, "mcp", (HttpServlet) transportProvider);
        ctx.addServletMappingDecoded("/*", "mcp");
        tomcat.getConnector();
        tomcat.start();
        System.out.println("Tomcat is running on port: " + tomcat.getConnector().getPort());
        tomcat.getServer().await();
    }

    private static McpServerFeatures.AsyncToolSpecification getToolSpecifiction() {
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