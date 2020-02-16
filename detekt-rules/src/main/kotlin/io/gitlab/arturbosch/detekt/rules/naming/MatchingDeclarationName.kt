package io.gitlab.arturbosch.detekt.rules.naming

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.isDataClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

/**
 * "If a Kotlin file contains a single non-private class (potentially with related top-level declarations),
 * its name should be the same as the name of the class, with the .kt extension appended.
 * If a file contains multiple classes, or only top-level declarations,
 * choose a name describing what the file contains, and name the file accordingly.
 * Use camel humps with an uppercase first letter (e.g. ProcessDeclarations.kt).
 *
 * The name of the file should describe what the code in the file does.
 * Therefore, you should avoid using meaningless words such as "Util" in file names." - Official Kotlin Style Guide
 *
 * More information at: http://kotlinlang.org/docs/reference/coding-conventions.html
 *
 * <noncompliant>
 *
 * class Foo // FooUtils.kt
 *
 * fun Bar.toFoo(): Foo = ...
 * fun Foo.toBar(): Bar = ...
 *
 * </noncompliant>
 *
 * <compliant>
 *
 * class Foo { // Foo.kt
 *     fun stuff() = 42
 * }
 *
 * fun Bar.toFoo(): Foo = ...
 *
 * </compliant>
 *
 * @configuration mustBeFirst - name should only be checked if the file starts with a class or object (default: `true`)
 * @configuration ignoreDataClasses - if data classes should be allowed (default: `true`)
 *
 * @active since v1.0.0
 */
class MatchingDeclarationName(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "If a source file contains only a single non-private top-level class or object, " +
            "the file name should reflect the case-sensitive name plus the .kt extension.",
        Debt.FIVE_MINS
    )

    private val ignoreDataClasses = valueOrDefault(IGNORE_DATA_CLASSES, true)
    private val mustBeFirst = valueOrDefault(MUST_BE_FIRST, true)

    override fun visitKtFile(file: KtFile) {
        val declarations = file.declarations
            .asSequence()
            .filterIsInstance<KtClassOrObject>()
            .filterNot { it.isPrivate() }
            .toList()

        fun matchesFirstClassOrObjectCondition(): Boolean =
            !mustBeFirst || mustBeFirst && declarations.first() === file.declarations.first()

        fun hasNoMatchingTypeAlias(filename: String): Boolean =
            file.declarations.filterIsInstance<KtTypeAlias>().all { it.name != filename }

        if (declarations.size == 1 && matchesFirstClassOrObjectCondition()) {
            val declaration = declarations.first()
            if (ignoreDataClasses && declaration.isDataClass()) {
                return
            }
            val declarationName = declaration.name
            val filename = file.name.removeSuffix(KOTLIN_SUFFIX)
            if (declarationName != filename && hasNoMatchingTypeAlias(filename)) {
                val entity = Entity.from(declaration).copy(ktElement = file)
                report(CodeSmell(issue, entity, "The file name '${file.name}' " +
                    "does not match the name of the single top-level declaration '$declarationName'."))
            }
        }
    }
}

const val IGNORE_DATA_CLASSES = "ignoreDataClasses"
const val MUST_BE_FIRST = "mustBeFirst"
private const val KOTLIN_SUFFIX = ".kt"
