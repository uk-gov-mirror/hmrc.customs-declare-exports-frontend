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

package views.declaration.officeOfExit

import base.Injector
import forms.declaration.officeOfExit.OfficeOfExitClearance
import forms.declaration.officeOfExit.OfficeOfExitForms.clearanceForm
import models.DeclarationType.CLEARANCE
import models.Mode
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.test.Helpers.stubMessages
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.declaration.officeOfExit.office_of_exit_clearance
import views.tags.ViewTest

@ViewTest
class OfficeOfExitClearanceViewSpec extends UnitViewSpec with ExportsTestData with Stubs with Injector {

  private val page: office_of_exit_clearance = new office_of_exit_clearance(mainTemplate)
  private val form: Form[OfficeOfExitClearance] = clearanceForm()
  private def createView(mode: Mode = Mode.Normal, form: Form[OfficeOfExitClearance] = form): Document =
    page(mode, form)(journeyRequest(CLEARANCE), stubMessages())

  "Office of Exit View for standard" should {
    val view = createView()

    "have proper messages for labels" in {
      val messages = instanceOf[MessagesApi].preferred(journeyRequest())
      messages must haveTranslationFor("declaration.officeOfExit.title")
      messages must haveTranslationFor("declaration.summary.locations.header")
      messages must haveTranslationFor("declaration.officeOfExit")
      messages must haveTranslationFor("declaration.officeOfExit.hint")
      messages must haveTranslationFor("declaration.officeOfExit.empty")
      messages must haveTranslationFor("declaration.officeOfExit.length")
      messages must haveTranslationFor("declaration.officeOfExit.specialCharacters")
      messages must haveTranslationFor("standard.officeOfExit.circumstancesCode")
      messages must haveTranslationFor("standard.officeOfExit.circumstancesCode.hint")
      messages must haveTranslationFor("standard.officeOfExit.circumstancesCode.empty")
      messages must haveTranslationFor("standard.officeOfExit.circumstancesCode.error")
    }

    "Office of Exit View on empty page for standard" should {

      "display page title" in {
        view.getElementById("title").text() mustBe "declaration.officeOfExit.title"
      }

      "display section header" in {
        view.getElementById("section-header").text() must include("declaration.summary.locations.header")
      }

      "display office of exit question" in {
        view.getElementById("officeId-label").text() mustBe "declaration.officeOfExit"
        view.getElementById("officeId-hint").text() mustBe "declaration.officeOfExit.hint"
        view.getElementById("officeId").attr("value") mustBe empty
      }

      "display circumstances code question" in {
        view.getElementById("circumstancesCode-label").text() mustBe "standard.officeOfExit.circumstancesCode"
        view.getElementById("circumstancesCode-hint").text() mustBe "standard.officeOfExit.circumstancesCode.hint"
      }

      "display 'Back' button that links to 'Location of Goods' page" in {

        val backButton = view.getElementById("back-link")

        backButton.text() mustBe "site.back"
        backButton.getElementById("back-link") must haveHref(controllers.declaration.routes.LocationController.displayPage(Mode.Normal))
      }

      "display 'Save and continue' button" in {
        val saveButton = view.getElementById("submit")
        saveButton.text() mustBe "site.save_and_continue"
      }

      "display 'Save and return' button on page" in {
        val saveAndReturnButton = view.getElementById("submit_and_return")
        saveAndReturnButton.text() mustBe "site.save_and_come_back_later"
      }
    }

    "Office of Exit during standard declaration for invalid input" should {

      "display errors when all inputs are empty" in {
        val data = OfficeOfExitClearance(None, "")
        val form = clearanceForm().fillAndValidate(data)
        val view = createView(form = form)

        checkErrorsSummary(view)

        view.getElementById("circumstancesCode-error").text() mustBe "standard.officeOfExit.circumstancesCode.error"
        view
          .getElementById("error-message-circumstancesCode-input")
          .text() mustBe "standard.officeOfExit.circumstancesCode.error"
      }

      "display errors when all inputs are incorrect" in {
        val data = OfficeOfExitClearance(Some("123456"), "Yes")
        val form = clearanceForm().fillAndValidate(data)
        val view = createView(form = form)

        checkErrorsSummary(view)

        view.getElementById("officeId-error").text() mustBe "declaration.officeOfExit.length"
        view.getElementById("error-message-officeId-input").text() mustBe "declaration.officeOfExit.length"
      }

      "display errors when office of exit contains special characters" in {
        val data = OfficeOfExitClearance(Some("12#$%^78"), "Yes")
        val form = clearanceForm().fillAndValidate(data)
        val view = createView(form = form)

        checkErrorsSummary(view)

        view.getElementById("officeId-error").text() mustBe "declaration.officeOfExit.specialCharacters"
        view.getElementById("error-message-officeId-input").text() mustBe "declaration.officeOfExit.specialCharacters"
      }
    }
  }
}