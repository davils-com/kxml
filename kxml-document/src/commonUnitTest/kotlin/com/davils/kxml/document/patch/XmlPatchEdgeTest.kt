/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.davils.kxml.document.patch

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlCData
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.fixture.documentOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val ROOT_COMMENT: String = "<!--a--><doc/><!--b-->"
private const val SINGLE_TEXT: String = "<doc>x</doc>"
private const val ONLY_TEXT: String = "doc/text()"
private const val PLAIN: String = "<doc/>"
private const val SELECTOR: String = "a"
private const val ELEMENT: String = "doc"
private const val INNER: String = "x"
private const val URN_X: String = "urn:x"
private const val GENERATED: String = "ns0"
private const val PREFIX_P: String = "doc/namespace::p"
private const val NEW_URI: String = "urn:new"

private val nothing: List<XmlNode> = emptyList()

private fun appliedOf(target: String, operations: List<XmlPatchOperation>): XmlDocument {
    val result = XmlPatch(operations).apply(documentOf(target))
    return result.shouldBeInstanceOf<XmlPatchResult.Applied>().document
}

private fun appliedOf(target: String, operation: XmlPatchOperation): XmlDocument = appliedOf(target, listOf(operation))

private fun codeOf(target: String, operation: XmlPatchOperation): XmlPatchErrorCode {
    val result = XmlPatch(listOf(operation)).apply(documentOf(target))
    return result.shouldBeInstanceOf<XmlPatchResult.Failed>().error.code
}

private fun error(operation: Int): XmlPatchError =
    XmlPatchError(code = XmlPatchErrorCode.UnlocatedNode, operation = operation, message = "m")

class XmlPatchEdgeTest : FunSpec({
    context("the nodes around the document element") {
        test("are added, replaced and removed") {
            val newRoot = listOf(xmlElement(XmlName("new")))
            val added = appliedOf(ROOT_COMMENT, XmlPatchOperation.Add(ELEMENT, nothing, XmlPatchPosition.After))
            val replaced = appliedOf(ROOT_COMMENT, XmlPatchOperation.Replace("/*", newRoot))
            val removed = appliedOf(ROOT_COMMENT, XmlPatchOperation.Remove("/comment()[2]"))

            added.epilog.size shouldBe 1
            replaced.root.name.local shouldBe "new"
            replaced.prolog.size shouldBe 1
            removed.epilog.size shouldBe 0
        }
    }

    context("names of the XML namespace and of no prefix") {
        test("are added without a declaration") {
            val operation = XmlPatchOperation.AddAttribute(selector = ELEMENT, name = "xml:lang", value = "en")
            val root = appliedOf(PLAIN, operation).root
            val attribute = root.attributes.single()

            attribute.name.namespace shouldBe XmlNamespaces.XML
            root.namespaces.size shouldBe 0
        }

        test("get a generated prefix when an added attribute has a namespace and no prefix") {
            val name = XmlName(local = SELECTOR, namespace = "urn:a")
            val element = xmlElement(XmlName("e")) { attribute(name, "1") }
            val added = appliedOf(PLAIN, XmlPatchOperation.Add(ELEMENT, listOf(element))).root.elements.single()

            val attribute = added.attributes.single()
            val declaration = added.namespaces.single()

            attribute.name.prefix shouldBe GENERATED
            declaration.prefix shouldBe GENERATED
        }

        test("take the prefix of their parent when the diff uses another one") {
            val operation = XmlPatchOperation.Add(
                selector = "*",
                content = listOf(xmlElement(XmlName(local = SELECTOR, namespace = URN_X, prefix = "z"))),
                namespaces = mapOf("z" to URN_X),
            )
            val added = appliedOf("<b:doc xmlns:a='urn:x' xmlns:b='urn:x'/>", operation).root.elements.single()

            added.name.prefix shouldBe "b"
        }
    }

    context("namespace declarations") {
        test("are rewritten where they are in scope and not where they are shadowed") {
            val target = "<doc xmlns:p='urn:p'><p:a p:x='1'><p:b xmlns:p='urn:other'/></p:a></doc>"
            val cdata = XmlPatchOperation.Replace(selector = PREFIX_P, content = listOf(xmlCData(INNER)))
            val renamed = appliedOf(target, XmlPatchOperation.Replace(PREFIX_P, listOf(xmlText(NEW_URI))))
            val root = renamed.root
            val outer = root.elements.single()
            val attribute = outer.attributes.single()
            val inner = outer.elements.single()

            codeOf(target, cdata) shouldBe XmlPatchErrorCode.InvalidNodeTypes
            root.namespaces.single().uri shouldBe NEW_URI
            outer.name.namespace shouldBe NEW_URI
            attribute.name.namespace shouldBe NEW_URI
            inner.name.namespace shouldBe "urn:other"
        }

        test("are removed only when nothing in scope uses them") {
            val removed = appliedOf(
                target = "<doc xmlns:p='urn:p'><a><p:b xmlns:p='urn:q'/></a></doc>",
                operation = XmlPatchOperation.Remove(PREFIX_P),
            )
            val used = codeOf("<doc xmlns:p='urn:p' p:a='1'/>", XmlPatchOperation.Remove(PREFIX_P))
            val default = codeOf("<doc xmlns='urn:d'/>", XmlPatchOperation.Remove("/*/namespace::*[name() = '']"))

            removed.root.namespaces.size shouldBe 0
            used shouldBe XmlPatchErrorCode.InvalidNamespacePrefix
            default shouldBe XmlPatchErrorCode.InvalidNamespacePrefix
        }
    }

    context("the kinds of nodes") {
        test("must fit the operation") {
            val element = listOf(xmlElement(XmlName(INNER)))
            val badValue = XmlPatchOperation.AddAttribute(selector = ELEMENT, name = SELECTOR, value = "\u0001")
            val onText = XmlPatchOperation.AddAttribute(selector = ONLY_TEXT, name = SELECTOR, value = "1")

            codeOf(SINGLE_TEXT, XmlPatchOperation.Add(ONLY_TEXT, nothing)) shouldBe XmlPatchErrorCode.InvalidNodeTypes
            codeOf(SINGLE_TEXT, onText) shouldBe XmlPatchErrorCode.InvalidNodeTypes
            codeOf(PLAIN, badValue) shouldBe XmlPatchErrorCode.InvalidAttributeValue
            codeOf("<doc a='1'/>", XmlPatchOperation.Replace("doc/@a", listOf(xmlCData(INNER)))) shouldBe
                XmlPatchErrorCode.InvalidNodeTypes
            val textByElement = XmlPatchOperation.Replace(ONLY_TEXT, element)
            codeOf(SINGLE_TEXT, textByElement) shouldBe XmlPatchErrorCode.InvalidNodeTypes
            codeOf(PLAIN, XmlPatchOperation.Remove("1")) shouldBe XmlPatchErrorCode.InvalidAttributeValue
        }

        test("need white space where a removal asks for it") {
            val after = XmlPatchOperation.Remove("doc/a", XmlPatchWhitespace.After)

            codeOf("<doc><a/></doc>", after) shouldBe XmlPatchErrorCode.InvalidWhitespaceDirective
            codeOf("<doc><a/><b/></doc>", after) shouldBe XmlPatchErrorCode.InvalidWhitespaceDirective
            codeOf("<doc><a/>x</doc>", after) shouldBe XmlPatchErrorCode.InvalidWhitespaceDirective
        }
    }

    context("selectors of namespace nodes") {
        test("cannot take content, attributes or siblings") {
            val target = "<doc xmlns:p='urn:p'/>"
            val selector = "doc/namespace::p"
            val attribute = XmlPatchOperation.AddAttribute(selector = selector, name = SELECTOR, value = "1")

            codeOf(target, XmlPatchOperation.Add(selector, nothing)) shouldBe XmlPatchErrorCode.InvalidNodeTypes
            codeOf(target, XmlPatchOperation.Add(selector, nothing, XmlPatchPosition.After)) shouldBe
                XmlPatchErrorCode.InvalidNodeTypes
            codeOf(target, attribute) shouldBe XmlPatchErrorCode.InvalidNodeTypes
        }
    }

    context("added content") {
        test("declares a default namespace the target lacks and undeclares one it has") {
            val qualified = xmlElement(XmlName(local = "e", namespace = NEW_URI))
            val operations = listOf(
                XmlPatchOperation.Add("/*", listOf(qualified)),
                XmlPatchOperation.Add("/*/*[1]", listOf(xmlElement(XmlName("u")))),
            )
            val document = appliedOf(target = "<doc xmlns='urn:d'><x xmlns=''/></doc>", operations = operations)
            val children = document.root.elements
            val inner = children.first().elements.single()
            val declared = children.last().namespaces.single()

            declared.uri shouldBe NEW_URI
            inner.namespaces.size shouldBe 0
        }

        test("joins its text with the text around it") {
            val added = listOf(xmlText("y"))
            val after = appliedOf(SINGLE_TEXT, XmlPatchOperation.Add(ONLY_TEXT, added, XmlPatchPosition.After))
            val before = appliedOf(SINGLE_TEXT, XmlPatchOperation.Add(ONLY_TEXT, added, XmlPatchPosition.Before))
            val beside = XmlPatchOperation.Add("/processing-instruction()", nothing, XmlPatchPosition.After)

            after.root.text shouldBe "xy"
            before.root.text shouldBe "yx"
            appliedOf("<?pi?><doc/>", beside).prolog.size shouldBe 1
        }
    }

    context("errors and operations") {
        test("compare by their content") {
            error(operation = 1) shouldBe error(operation = 1)
            error(operation = 1).hashCode() shouldBe error(operation = 1).hashCode()
            error(operation = 1) shouldNotBe error(operation = 2)
            error(operation = 1).equals("m") shouldBe false
            XmlPatchOperation.Replace(SELECTOR, nothing) shouldBe XmlPatchOperation.Replace(SELECTOR, nothing)
            XmlPatchOperation.Remove(SELECTOR) shouldNotBe XmlPatchOperation.Remove(SELECTOR, XmlPatchWhitespace.Before)
            val remove = XmlPatchOperation.Remove(SELECTOR)
            val replace = XmlPatchOperation.Replace(SELECTOR, nothing)
            remove.hashCode() shouldNotBe replace.hashCode()
            XmlPatch(emptyList()) shouldNotBe XmlPatch(emptyList(), XmlLimits.Relaxed)
        }

        test("describe themselves by kind and selector") {
            val namespace = XmlPatchOperation.AddNamespace(selector = SELECTOR, prefix = "p", uri = "urn:p")
            val attribute = XmlPatchOperation.AddAttribute(selector = SELECTOR, name = "b", value = "c")

            namespace.toString() shouldBe "AddNamespace(a)"
            attribute.toString() shouldBe "AddAttribute(a)"
            val same = XmlPatchOperation.AddAttribute(selector = SELECTOR, name = "b", value = "c")
            attribute.hashCode() shouldBe same.hashCode()
            XmlPatchOperation.Replace(SELECTOR, nothing).toString() shouldBe "Replace(a)"
            XmlPatchOperation.Remove(SELECTOR).toString() shouldBe "Remove(a)"
            shouldThrow<IllegalArgumentException> {
                XmlPatchOperation.AddNamespace(selector = SELECTOR, prefix = "1a", uri = URN_X)
            }
        }

        test("read texts and CDATA sections as the diff holds them") {
            val reading = XmlPatch.read(documentOf("<diff><![CDATA[ ]]><add sel='doc'><![CDATA[x]]></add></diff>"))
            val patch = reading.shouldBeInstanceOf<XmlPatchReading.Read>().patch
            val refused = XmlPatch.read(documentOf("<diff><![CDATA[x]]></diff>"))

            patch.operations.map { operation -> operation.selector } shouldContainExactly listOf(ELEMENT)
            val error = refused.shouldBeInstanceOf<XmlPatchReading.Refused>().error
            error.code shouldBe XmlPatchErrorCode.InvalidDiffFormat
        }
    }
})
