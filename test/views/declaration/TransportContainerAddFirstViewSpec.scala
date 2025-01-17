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

package views.declaration

import base.Injector
import forms.declaration.ContainerFirst
import models.Mode
import org.jsoup.nodes.Document
import org.scalatest.MustMatchers
import play.api.data.Form
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.components.gds.Styles
import views.declaration.spec.UnitViewSpec
import views.helpers.CommonMessages
import views.html.declaration.transport_container_add_first
import views.tags.ViewTest

@ViewTest
class TransportContainerAddFirstViewSpec extends UnitViewSpec with ExportsTestData with Stubs with MustMatchers with CommonMessages with Injector {

  private val form: Form[ContainerFirst] = ContainerFirst.form()
  private val page = instanceOf[transport_container_add_first]

  private def createView(form: Form[ContainerFirst] = form): Document =
    page(Mode.Normal, form)(journeyRequest(), messages)

  "Transport Containers Add First View" should {
    val view = createView()

    "display page title" in {
      view.getElementsByClass(Styles.gdsPageLegend) must containMessageForElements("declaration.transportInformation.containers.first.title")
    }

    "display section header" in {
      view.getElementById("section-header") must containMessage("declaration.section.6")
    }

    "display 'Back' button that links to 'transport payment' page" in {
      val backLinkContainer = view.getElementById("back-link")

      backLinkContainer.getElementById("back-link") must haveHref(controllers.declaration.routes.TransportPaymentController.displayPage(Mode.Normal))
    }

    "display 'Save and continue' button on page" in {
      val saveButton = view.getElementById("submit")
      saveButton must containMessage(saveAndContinueCaption)
    }

    "display 'Save and return' button on page" in {
      val saveAndReturnButton = view.getElementById("submit_and_return")
      saveAndReturnButton must containMessage(saveAndReturnCaption)
    }
  }

  "Transport Containers Add View" should {

    "display errors for invalid input" in {
      val view = createView(ContainerFirst.form().fillAndValidate(ContainerFirst(Some("abc123@#"))))

      view must haveGovukGlobalErrorSummary
      view must containErrorElementWithTagAndHref("a", "#id")

      view must containErrorElementWithMessageKey("declaration.transportInformation.containerId.error.invalid")
    }

    "display errors for invalid length" in {
      val view = createView(ContainerFirst.form().fillAndValidate(ContainerFirst(Some("123456789012345678"))))

      view must haveGovukGlobalErrorSummary
      view must containErrorElementWithTagAndHref("a", "#id")

      view must containErrorElementWithMessageKey("declaration.transportInformation.containerId.error.length")
    }
  }

  "Transport Containers Add View when filled" should {

    "display data in Container ID input" in {

      val view = createView(ContainerFirst.form().fill(ContainerFirst(Some("Test"))))

      view.getElementById("id").attr("value") must be("Test")
    }
  }
}
