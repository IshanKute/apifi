package apifi.codegen

import apifi.codegen.security.BasicAuthSecurityStubBuilder
import apifi.codegen.security.BearerAuthSecurityStubBuilder
import apifi.parser.models.CommonSpec
import apifi.parser.models.SecurityDefinition
import apifi.parser.models.SecurityDefinitionType
import apifi.parser.models.Spec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

object CodeGenerator {
    fun generate(spec: Spec, basePackageName: String): List<FileSpec> {
        val modelFiles: List<FileSpec> = if(spec.models.isNotEmpty()) listOf(ModelFileBuilder.build(spec.models, basePackageName)) else emptyList()
        val modelMapping = modelFiles.flatMap { it.members.mapNotNull { m -> (m as TypeSpec).name }.map { name -> name to "${it.packageName}.$name" } }
        val securityFiles = spec.securityDefinitions.fold<SecurityDefinition, Map<SecurityDefinition, FileSpec>>(mapOf(), { acc, securityDefinition ->
            when(securityDefinition.type) {
                SecurityDefinitionType.BASIC_AUTH -> acc + mapOf(securityDefinition to BasicAuthSecurityStubBuilder.build(basePackageName))
                SecurityDefinitionType.BEARER -> acc + mapOf(securityDefinition to BearerAuthSecurityStubBuilder.build(basePackageName))
            }
        })

        val securityDependencies = securityFiles
                .filter { spec.securityRequirements.contains(it.key.name) }
                .map { (def, spec) -> SecurityDependency((spec.members.first() as TypeSpec).name!!, spec.packageName, def.type) }

        val controllerFiles = spec.paths.map { path ->
            ControllerBuilder.build(path, securityDependencies, basePackageName, modelMapping)
        }

        return (controllerFiles + modelFiles)
    }

    fun generateCommon(spec: CommonSpec, basePackageName: String): CommonFileContent {
        val modelFiles: List<FileSpec> = if(spec.models.isNotEmpty()) listOf(ModelFileBuilder.build(spec.models, basePackageName)) else emptyList()

        val securityFiles = spec.securityDefinitions.fold<SecurityDefinition, Map<SecurityDefinition, FileSpec>>(mapOf(), { acc, securityDefinition ->
            when(securityDefinition.type) {
                SecurityDefinitionType.BASIC_AUTH -> acc + mapOf(securityDefinition to BasicAuthSecurityStubBuilder.build(basePackageName))
                SecurityDefinitionType.BEARER -> acc + mapOf(securityDefinition to BearerAuthSecurityStubBuilder.build(basePackageName))
            }
        })

        return CommonFileContent(modelFiles, securityFiles)
    }

}

data class CommonFileContent(
        val modelFiles: List<FileSpec>,
        val securityFiles: Map<SecurityDefinition, FileSpec>
)

data class SecurityDependency(val name: String, val packageName: String, val securityDefinitionType: SecurityDefinitionType)
