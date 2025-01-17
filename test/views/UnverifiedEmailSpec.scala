/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import base.Injector
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.helpers.CommonMessages
import views.html.unverified_email

import scala.collection.JavaConverters._

class UnverifiedEmailSpec extends UnitViewSpec with CommonMessages with Stubs with Injector {

  lazy val redirectUrl = "/some/url"

  val page = instanceOf[unverified_email]

  val view = () => page(redirectUrl)(request, messages)

  val messageKeyPrefix = "emailUnverified"

  "Unverified Email Page" must {

    "display page header" in {
      view().getElementsByTag("h1").first() must containMessage(s"$messageKeyPrefix.heading")
    }

    "have a 'Verify your email address' button with correct link" in {
      val link = view().getElementsByClass("govuk-button").first()

      link must haveHref(redirectUrl)
      link must containMessage(s"${messageKeyPrefix}.link")
    }

    "have paragraph1 with text" in {
      view().getElementById("emailUnverified.para1") must containMessage(s"${messageKeyPrefix}.paragraph1")
    }

    "have bullet list with expected items" in {
      val ul = view().getElementById("emailUnverified.bullets")

      val expectedBulletTextKeys = (1 to 4).map { idx =>
        s"${messageKeyPrefix}.bullets.item${idx}"
      }

      val itemsWithExpectedKeys = ul.children().asScala.zip(expectedBulletTextKeys)

      itemsWithExpectedKeys.foreach { itemWithExpected =>
        val (item, expectedKey) = itemWithExpected
        item must containMessage(expectedKey)
      }
    }
  }
}
