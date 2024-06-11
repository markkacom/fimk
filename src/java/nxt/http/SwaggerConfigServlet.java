package nxt.http;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class SwaggerConfigServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        OpenAPI oas = new OpenAPI().info(new Info()
                .title("Your API Title")
                .description("Your API Description")
                .version("1.0.0"));
        SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration().openAPI(oas);

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .servletConfig(config)
                    //.application(null) // Replace with your JAX-RS Application class if you have one
                    .openApiConfiguration(swaggerConfiguration)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
