/*
 * Copyright 2020 HM Revenue & Customs
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

package views.declaration

import base.Injector
import forms.common.YesNoAnswer
import forms.declaration.Seal
import helpers.views.declaration.CommonMessages
import models.Mode
import org.jsoup.nodes.Document
import org.scalatest.MustMatchers
import play.api.data.Form
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.declaration.seal_summary
import views.tags.ViewTest

@ViewTest
class SealSummaryViewSpec extends UnitViewSpec with Stubs with MustMatchers with CommonMessages with Injector {

  val containerId = "212374"
  val sealId = "76434574"
  val seals = Seq(Seal(sealId))
  private val form: Form[YesNoAnswer] = YesNoAnswer.form()
  private val page = instanceOf[seal_summary]

  private def createView(form: Form[YesNoAnswer] = form): Document =
    page(Mode.Normal, form, containerId, seals)

  "Seal Summary View" should {
    val view = createView()

    "display page title" in {
      view.getElementsByTag("h1").text() must be(messages("declaration.seal.title"))
    }

    "display summary of seals" in {
      view.getElementById("removable_elements-row0-label").text() must be(sealId)
    }

    "display 'Back' button that links to 'containers summary' page" in {
      val backLinkContainer = view.getElementById("back-link")

      backLinkContainer.text() must be(messages(backCaption))
      backLinkContainer.getElementById("back-link") must haveHref(
        controllers.declaration.routes.TransportContainerController.displayContainerSummary(Mode.Normal)
      )
    }

    "display 'Save and continue' button on page" in {
      val saveButton = view.getElementById("submit")
      saveButton.text() must be(messages(saveAndContinueCaption))
    }

    "display 'Save and return' button on page" in {
      val saveAndReturnButton = view.getElementById("submit_and_return")
      saveAndReturnButton.text() must be(messages(saveAndReturnCaption))
    }
  }

  "Seal Summary View for invalid input" should {

    "display error if nothing is entered" in {
      val view = createView(YesNoAnswer.form().bind(Map[String, String]()))

      view must haveGovukGlobalErrorSummary
      view must containErrorElementWithTagAndHref("a", "#yesNo")

      view must containErrorElementWithMessage("error.yesNo.required")
    }

  }
}
