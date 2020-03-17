/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import java.util.*

class GlobalInlineContext(private val diagnostics: DiagnosticSink) {
    private val processing = linkedMapOf<CallableDescriptor, PsiElement?>()

    private val typesUsedInInlineFunctions = LinkedList<MutableSet<String>>()

    fun enterIntoInlining(context: CallableDescriptor, callee: CallableDescriptor?, element: PsiElement?): Boolean {
        assert(context.original !in processing) { "entered inlining cycle on $context" }
        processing[context.original] = element
        if (callee?.original in processing) {
            var currentElement = element
            for ((callContainer, callElement) in processing.entries.dropWhile { it.key != callee?.original }) {
                currentElement?.let { diagnostics.report(Errors.INLINE_CALL_CYCLE.on(it, callContainer)) }
                currentElement = callElement // next container is the target of this call
            }
            processing.remove(context.original)
            return false
        }
        typesUsedInInlineFunctions.push(hashSetOf())
        return true
    }

    fun exitFromInlining(context: CallableDescriptor) {
        processing.remove(context.original)
        val pop = typesUsedInInlineFunctions.pop()
        typesUsedInInlineFunctions.peek()?.addAll(pop)
    }

    fun recordTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().add(type)

    fun isTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().contains(type)
}