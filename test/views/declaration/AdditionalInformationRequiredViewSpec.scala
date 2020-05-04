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
import controllers.declaration.routes
import controllers.util.SaveAndReturn
import forms.common.YesNoAnswer
import forms.common.YesNoAnswer.YesNoAnswers
import helpers.views.declaration.CommonMessages
import models.DeclarationType.DeclarationType
import models.requests.JourneyRequest
import models.Mode
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.MessagesApi
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.declaration.additional_information_required
import views.tags.ViewTest

@ViewTest
class AdditionalInformationRequiredViewSpec extends UnitViewSpec with ExportsTestData with CommonMessages with Stubs with Injector {
  val itemId = "a7sc78"
  private def form(journeyType:DeclarationType): Form[YesNoAnswer] = YesNoAnswer.form()
  private val additionalInfoReqPage = instanceOf[additional_information_required]
  private def createView(form: Form[YesNoAnswer])
                        (implicit request: JourneyRequest[_])
                            : Document = additionalInfoReqPage(Mode.Normal,itemId, form)

  "Additional Information Required View on empty page" should {

    "have correct message keys" in {

      val messages = instanceOf[MessagesApi].preferred(request)

      messages must haveTranslationFor("declaration.additionalInformationRequired.title")
      messages must haveTranslationFor("declaration.summary.parties.header")
      messages must haveTranslationFor("declaration.additionalInformationRequired.error")
      messages must haveTranslationFor("declaration.additionalInformationRequired.hint")

    }
  }

  "Additional Information Required View on empty page" should {
    val view = createView()

    onEveryDeclarationJourney() {implicit request =>
      "display page title" in {
        createView(form(request.declarationType)).getElementById("h1").text() mustBe messages("declaration.additionalInformationRequired.title")
      }


    "display section header" in {
      createView(form(request.declarationType)).getElementById("section-header").text() mustBe messages("declaration.summary.parties.header")
    }

      "display radio button with Yes option" in {
        val view = createView(form(request.declarationType))
        view.getElementById("code_yes").attr("value") mustBe YesNoAnswers.yes
        view.getElementsByAttributeValue("for", "code_yes").text() mustBe "site.yes"
      }
      "display radio button with No option" in {
        val view = createView(form(request.declarationType))
        view.getElementById("code_no").attr("value") mustBe YesNoAnswers.no
        view.getElementsByAttributeValue("for", "code_no").text() mustBe "site.no"
      }


      "display 'Back' button that links to the 'Commodity Measure' page" in {

        val  backLinkContainer = view.getElementById("back-link")
        val backButton = view.getElementById("back-link")

        backButton.text() mustBe messages(backCaption)
        backButton.attr("href") mustBe routes.CONFIRM.displayPage().url
      }

      "display 'Save and continue' button on page" in {
        val saveButton = createView(form(request.declarationType)).getElementById("submit")
        saveButton.text() mustBe messages(saveAndContinueCaption)
      }

      "display 'Save and return' button on page" in {
        val saveButton = createView(form(request.declarationType)).getElementById("submit_and_return")
        saveButton.text() mustBe messages(saveAndReturnCaption)
        saveButton.attr("name") mustBe SaveAndReturn.toString
      }
  }
}
}
