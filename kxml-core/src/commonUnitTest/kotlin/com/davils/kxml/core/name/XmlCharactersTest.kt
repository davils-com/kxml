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

package com.davils.kxml.core.name

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LAST_CODE_POINT: Int = 0x10FFFF
private const val NOT_A_CHARACTER: Int = 0xFFFE
private const val MIDDLE_DOT: Int = 0xB7
private const val SUPPLEMENTARY_NAME_START: Int = 0x10000

class XmlCharactersTest : FunSpec({
    context("characters") {
        test("are the code points of the production Char") {
            XmlCharacters.isCharacter('\t'.code) shouldBe true
            XmlCharacters.isCharacter(LAST_CODE_POINT) shouldBe true
            XmlCharacters.isCharacter(0) shouldBe false
            XmlCharacters.isCharacter(NOT_A_CHARACTER) shouldBe false
            XmlCharacters.isCharacter(LAST_CODE_POINT + 1) shouldBe false
        }

        test("are white space only as the production S allows") {
            " \t\n\r".all(XmlCharacters::isWhitespace) shouldBe true
            XmlCharacters.isWhitespace('\u000C') shouldBe false
            XmlCharacters.isWhitespace(' ') shouldBe false
        }
    }

    context("names") {
        test("start and continue by the productions NameStartChar and NameChar") {
            XmlCharacters.isNameStart('_'.code) shouldBe true
            XmlCharacters.isNameStart(':'.code) shouldBe true
            XmlCharacters.isNameStart(SUPPLEMENTARY_NAME_START) shouldBe true
            XmlCharacters.isNameStart('1'.code) shouldBe false
            XmlCharacters.isNameCharacter('1'.code) shouldBe true
            XmlCharacters.isNameCharacter(MIDDLE_DOT) shouldBe true
            XmlCharacters.isNameCharacter(' '.code) shouldBe false
        }

        test("are NCNames without a colon") {
            XmlCharacters.isNcName("a-b.c") shouldBe true
            XmlCharacters.isNcName("𐀀x") shouldBe true
            XmlCharacters.isNcName("a:b") shouldBe false
            XmlCharacters.isNcName("1a") shouldBe false
            XmlCharacters.isNcName("") shouldBe false
        }
    }
})
