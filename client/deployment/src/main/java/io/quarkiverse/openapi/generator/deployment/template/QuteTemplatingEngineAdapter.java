package io.quarkiverse.openapi.generator.deployment.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.openapitools.codegen.api.AbstractTemplatingEngineAdapter;
import org.openapitools.codegen.api.TemplatingExecutor;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;

public class QuteTemplatingEngineAdapter extends AbstractTemplatingEngineAdapter {
    
    public static final String IDENTIFIER = "qute";
    public static final String[] INCLUDE_TEMPLATES = {
      "additionalEnumTypeAnnotations.qute",
      "additionalEnumTypeUnexpectedMember.qute",
      "additionalModelTypeAnnotations.qute",
      "beanValidation.qute",
      "beanValidationCore.qute",
      "beanValidationInlineCore.qute",
      "beanValidationHeaderParams.qute",
      "bodyParams.qute",
      "enumClass.qute",
      "enumOuterClass.qute",
      "headerParams.qute",
      "pathParams.qute",
      "cookieParams.qute",
      "pojo.qute",
      "pojoQueryParam.qute",
      "queryParams.qute",
      "auth/compositeAuthenticationProvider.qute",
      "auth/headersFactory.qute",
      "multipartFormdataPojo.qute",
      "pojoAdditionalProperties.qute",
      "operationJavaDoc.qute"
    };
    public static final String TEMPLATE_DIRECTORY = ConfigProvider.getConfig()
      .getOptionalValue("quarkus.openapi-generator.codegen.template-base-dir", String.class)
      .orElse("src/main/resources/templates");
    public final Engine engine;
    private final List<String> includeTemplates;
    
    public QuteTemplatingEngineAdapter() {
        this.engine = Engine.builder()
          .addDefaults()
          .addValueResolver(new ReflectionValueResolver())
          .addNamespaceResolver(OpenApiNamespaceResolver.INSTANCE)
          .addNamespaceResolver(StrNamespaceResolver.INSTANCE)
          .removeStandaloneLines(true)
          .strictRendering(true)
          .build();
        
        this.includeTemplates = loadTemplateFiles();
    }
    
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
    
    @Override
    public String[] getFileExtensions() {
        return new String[] { IDENTIFIER };
    }
    
    @Override
    public String compileTemplate(TemplatingExecutor executor, Map<String, Object> bundle, String templateFile)
      throws IOException {
        this.cacheTemplates(executor);
        Template template = engine.getTemplate(templateFile);
        if (template == null) {
            template = engine.parse(executor.getFullTemplateContents(templateFile));
            engine.putTemplate(templateFile, template);
        }
        return template.data(bundle).render();
    }
    
    public void cacheTemplates(TemplatingExecutor executor) {
        for (String templateId : INCLUDE_TEMPLATES) {
            Template incTemplate = engine.getTemplate(templateId);
            if (incTemplate == null) {
                incTemplate = engine.parse(executor.getFullTemplateContents(templateId));
                engine.putTemplate(templateId, incTemplate);
            }
        }
    }
    
    private List<String> loadTemplateFiles() {
        Path templateDirPath = Paths.get(TEMPLATE_DIRECTORY);
        List<String> templates = new ArrayList<>();
        
        if (Files.exists(templateDirPath) && Files.isDirectory(templateDirPath)) {
            try (Stream<Path> paths = Files.list(templateDirPath)) {
                templates = paths.filter(Files::isRegularFile)
                  .map(Path::getFileName)
                  .map(Path::toString)
                  .map(name -> name.replace("\\", "/"))
                  .filter(name -> name.endsWith(".qute"))
                  .collect(Collectors.toCollection(ArrayList::new));
            } catch (IOException e) {
                throw new RuntimeException("Error while loading qute templates from: " + TEMPLATE_DIRECTORY, e);
            }
        }
        
        for (String defaultTemplate : INCLUDE_TEMPLATES) {
            if (!templates.contains(defaultTemplate)) {
                templates.add(defaultTemplate);
            }
        }
        return templates;
    }
}