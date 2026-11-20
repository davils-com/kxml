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

import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.c14n.XmlCanonicalization
import com.davils.kxml.document.c14n.XmlCanonicalizer
import com.davils.kxml.document.fixture.documentOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val sample: String = """
    |<doc>
    |  <note>This is a sample document</note>
    |</doc>
""".trimMargin()
private const val CHILD_ELEMENT: String = """<foo id="ert4773">This is a new child</foo>"""
private val withChild: String = """
    |<doc>
    |  <note>This is a sample document</note>
    |$CHILD_ELEMENT</doc>
""".trimMargin()
private val foo: String = """
    |<doc>
    |  <foo a="1">This is a sample document</foo>
    |</doc>
""".trimMargin()
private val fooWithoutText: String = """
    |<doc>
    |  <foo a="1"/>
    |</doc>
""".trimMargin()
private const val SAMPLE_TEXT: String = "This is a sample document"

private val canonicalizer: XmlCanonicalizer = XmlCanonicalizer(XmlCanonicalization.Canonical10WithComments)

private fun canonicalOf(document: XmlDocument): String = canonicalizer.canonicalize(document).decodeToString()

internal fun patched(target: String, operations: String): XmlPatchResult {
    val diff = documentOf("<diff>$operations</diff>")
    val patch = XmlPatch.read(diff).shouldBeInstanceOf<XmlPatchReading.Read>().patch
    return patch.apply(documentOf(target))
}

private fun shouldPatch(target: String, operations: String, expected: String) {
    val applied = patched(target, operations).shouldBeInstanceOf<XmlPatchResult.Applied>()
    canonicalOf(applied.document) shouldBe canonicalOf(documentOf(expected))
}

class XmlPatchTest : FunSpec({
    context("the additions of appendix A of RFC 5261") {
        test("add an element, an attribute, a namespace and a comment") {
            shouldPatch(target = sample, operations = """<add sel="doc">$CHILD_ELEMENT</add>""", expected = withChild)
            shouldPatch(
                target = withChild,
                operations = """<add sel="doc/foo[@id='ert4773']" type="@user">Bob</add>""",
                expected = withChild.replace(oldValue = """id="ert4773"""", newValue = """id="ert4773" user="Bob""""),
            )
            shouldPatch(
                target = withChild,
                operations = """<add sel="doc" type="namespace::pref">urn:ns:xxx</add>""",
                expected = withChild.replace(oldValue = "<doc>", newValue = """<doc xmlns:pref="urn:ns:xxx">"""),
            )
            shouldPatch(
                target = withChild,
                operations = """<add sel="doc/foo[@id='ert4773']" pos="before"><!-- comment --></add>""",
                expected = withChild.replace(oldValue = "<foo", newValue = "<!-- comment --><foo"),
            )
        }

        test("add several nodes and join adjacent text") {
            shouldPatch(
                target = sample,
                operations = "<add sel='doc'>\n  $CHILD_ELEMENT</add>",
                expected = sample.replace(oldValue = "</doc>", newValue = "\n  $CHILD_ELEMENT</doc>"),
            )
            shouldPatch(
                target = "<doc><a/>x</doc>",
                operations = "<add sel='doc/text()' pos='after'>new<bar/>elem</add>",
                expected = "<doc><a/>xnew<bar/>elem</doc>",
            )
            shouldPatch(
                target = "<doc><a/></doc>",
                operations = "<add sel='doc' pos='prepend'><?pi d?></add>",
                expected = "<doc><?pi d?><a/></doc>",
            )
        }
    }

    context("the replacements of appendix A of RFC 5261") {
        test("replace an element and an attribute") {
            shouldPatch(
                target = foo,
                operations = """<replace sel="doc/foo[@a='1']"><bar a="2"/></replace>""",
                expected = "<doc>\n  <bar a='2'/>\n</doc>",
            )
            shouldPatch(
                target = "<doc a='test'/>",
                operations = "<replace sel='doc/@a'>new</replace>",
                expected = "<doc a='new'/>",
            )
            shouldPatch(target = "<doc a='test'/>", operations = "<replace sel='doc/@a'/>", expected = "<doc a=''/>")
        }

        test("replace a namespace and the names that use it") {
            shouldPatch(
                target = "<doc xmlns:pref='urn:test'><pref:foo pref:a='1'/></doc>",
                operations = "<replace sel='doc/namespace::pref'>urn:new:xxx</replace>",
                expected = "<doc xmlns:pref='urn:new:xxx'><pref:foo pref:a='1'/></doc>",
            )
            shouldPatch(
                target = "<doc xmlns='urn:test'><foo/><p xmlns='urn:other'/></doc>",
                operations = "<replace sel='/*/namespace::*[name() = \"\"]'>urn:new</replace>",
                expected = "<doc xmlns='urn:new'><foo/><p xmlns='urn:other'/></doc>",
            )
        }

        test("replace a comment, an instruction and a text") {
            shouldPatch(
                target = "<doc><!-- comment --></doc>",
                operations = "<replace sel='doc/comment()[1]'><!-- new --></replace>",
                expected = "<doc><!-- new --></doc>",
            )
            shouldPatch(
                target = "<doc><?test foo='bar'?></doc>",
                operations = """<replace sel='doc/processing-instruction("test")'><?test bar='foobar'?></replace>""",
                expected = "<doc><?test bar='foobar'?></doc>",
            )
            shouldPatch(
                target = foo,
                operations = "<replace sel='doc/foo/text()[1]'>new text</replace>",
                expected = foo.replace(oldValue = SAMPLE_TEXT, newValue = "new text"),
            )
            shouldPatch(target = foo, operations = "<replace sel='doc/foo/text()[1]'/>", expected = fooWithoutText)
        }
    }

    context("the removals of appendix A of RFC 5261") {
        test("remove an element with the white space around it") {
            shouldPatch(target = foo, operations = "<remove sel='doc/foo' ws='after'/>", expected = "<doc>\n  </doc>")
            shouldPatch(target = foo, operations = "<remove sel='doc/foo' ws='both'/>", expected = "<doc></doc>")
            shouldPatch(target = foo, operations = "<remove sel='doc/foo' ws='before'/>", expected = "<doc>\n</doc>")
            shouldPatch(
                target = "<doc><a/>x<b/>y</doc>",
                operations = "<remove sel='doc/b'/>",
                expected = "<doc><a/>xy</doc>",
            )
        }

        test("remove an attribute, a namespace, a comment, an instruction and a text") {
            shouldPatch(target = "<doc a='foo'/>", operations = "<remove sel='doc/@a'/>", expected = "<doc/>")
            shouldPatch(
                target = "<doc><foo a='1' xmlns:pref='urn:test'>x</foo></doc>",
                operations = "<remove sel='doc/foo/namespace::pref'/>",
                expected = "<doc><foo a='1'>x</foo></doc>",
            )
            shouldPatch(
                target = "<doc><foo/>\n<!-- comment -->\n</doc>",
                operations = "<remove sel='doc/comment()[1]' ws='after'/>",
                expected = "<doc><foo/>\n</doc>",
            )
            shouldPatch(
                target = "<doc><foo/><?test?></doc>",
                operations = """<remove sel='doc/processing-instruction("test")'/>""",
                expected = "<doc><foo/></doc>",
            )
            shouldPatch(target = foo, operations = "<remove sel='doc/foo/text()[1]'/>", expected = fooWithoutText)
        }
    }

    context("namespace mangling") {
        test("maps the prefixes of the diff onto the namespaces of the target") {
            val target = "<doc xmlns='urn:x' xmlns:z='urn:y'><note>sample</note><elem a='foo'><child/></elem>" +
                "<elem a='bar'> <z:child/> </elem></doc>"
            val diff = "<p:diff xmlns='urn:x' xmlns:y='urn:y' xmlns:p='urn:diff'>" +
                """<p:add sel="doc/elem[@a='foo']"><!-- new --><child id='ert4773'><y:node/></child></p:add>""" +
                "<p:replace sel='doc/note/text()'>Patched doc</p:replace>" +
                """<p:remove sel="*/elem[@a='bar']/y:child" ws='both'/>""" +
                "<p:add sel=\"*/elem[@a='bar']\" type='@b'>new attr</p:add></p:diff>"
            val expected = "<doc xmlns='urn:x' xmlns:z='urn:y'><note>Patched doc</note><elem a='foo'><child/>" +
                "<!-- new --><child id='ert4773'><z:node/></child></elem><elem a='bar' b='new attr'></elem></doc>"
            val patch = XmlPatch.read(documentOf(diff)).shouldBeInstanceOf<XmlPatchReading.Read>().patch
            val applied = patch.apply(documentOf(target)).shouldBeInstanceOf<XmlPatchResult.Applied>()

            canonicalOf(applied.document) shouldBe canonicalOf(documentOf(expected))
            applied.toString() shouldBe "Applied"
        }
    }
})
