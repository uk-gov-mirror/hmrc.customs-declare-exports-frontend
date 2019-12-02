/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.declaration.InlandModeOfTransportCode
import models.Mode
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.declaration.inland_transport_details
import views.tags.ViewTest

@ViewTest
class InlandTransportDetailsViewSpec extends UnitViewSpec with ExportsTestData with Stubs {

  private val page = new inland_transport_details(mainTemplate)
  private val form: Form[InlandModeOfTransportCode] = InlandModeOfTransportCode.form()

  private def createView(mode: Mode = Mode.Normal, form: Form[InlandModeOfTransportCode] = form, messages: Messages = stubMessages()): Document =
    page(mode, form)(journeyRequest(), messages)

  "Inland Transport Details View" should {
    val view = createView()

    "have proper messages for labels" in {
      val messages = realMessagesApi.preferred(journeyRequest())
      messages must haveTranslationFor("declaration.warehouse.inlandTransportDetails.sectionHeader")
      messages must haveTranslationFor("declaration.warehouse.inlandTransportDetails.title")
      messages must haveTranslationFor("declaration.warehouse.inlandTransportDetails.hint")
      messages must haveTranslationFor("declaration.warehouse.inlandTransportDetails.error.incorrect")
    }

    "display same page title as header" in {
      val viewWithMessage = createView(messages = realMessagesApi.preferred(request))
      viewWithMessage.title() must include(viewWithMessage.getElementsByTag("h1").text())
    }

    "display 'Back' button that links to 'Supervising Customs Office' page" in {
      val backButton = view.getElementById("back-link")

      backButton.text() mustBe "site.back"
      backButton.getElementById("back-link") must haveHref(controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage(Mode.Normal))
    }

    "display 'Save and continue' button on page" in {
      view.getElementById("submit").text() mustBe "site.save_and_continue"
    }

    "display 'Save and return' button on page" in {
      view.getElementById("submit_and_return").text() mustBe "site.save_and_come_back_later"
    }
  }
}