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

package unit.controllers.declaration

import controllers.declaration.DestinationCountryController
import forms.declaration.countries.Country
import models.DeclarationType.DeclarationType
import models.{DeclarationType, Mode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.data.Form
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{AnyContentAsEmpty, Call, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.destinationCountries.destination_country

import scala.concurrent.ExecutionContext.global

class DestinationCountryControllerSpec extends ControllerSpec {

  val destinationCountryPage = mock[destination_country]

  val controller = new DestinationCountryController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    mockExportsCacheService,
    navigator,
    stubMessagesControllerComponents(),
    destinationCountryPage
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    authorizedUser()
    when(destinationCountryPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(destinationCountryPage)

    super.afterEach()
  }

  def theResponseForm: Form[Country] = {
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Country]])
    verify(destinationCountryPage).apply(any(), formCaptor.capture())(any(), any())
    formCaptor.getValue
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration())
    await(controller.displayPage(Mode.Normal)(request))
    theResponseForm
  }

  "Destination Country Controller" should {
    "return 200 (OK)" when {

      "display page method is invoked and cache is empty" in {

        withNewCaching(aDeclaration())

        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe OK
        verify(destinationCountryPage).apply(any(), any())(any(), any())
      }

      "display page method is invoked and cache contains data" in {

        withNewCaching(aDeclaration(withDestinationCountries()))

        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe OK
        verify(destinationCountryPage).apply(any(), any())(any(), any())
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "form contains incorrect country" in {

        withNewCaching(aDeclaration())

        val incorrectForm = JsObject(Map("countryCode" -> JsString("incorrect")))

        val result = controller.submit(Mode.Normal)(postRequest(incorrectForm))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 303 (SEE_OTHER) and redirect" when {

      def redirectForDeclarationType(declarationType: DeclarationType, redirect: Call): Unit =
        "redirect" in {
          withNewCaching(aDeclaration(withType(declarationType), withDestinationCountries()))

          val correctForm = JsObject(Map("countryCode" -> JsString("PL")))

          val result = controller.submit(Mode.Normal)(postRequest(correctForm))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe redirect
        }

      "submit for Standard declaration" should {
        behave like redirectForDeclarationType(
          DeclarationType.STANDARD,
          controllers.declaration.routes.RoutingCountriesController.displayRoutingQuestion()
        )
      }

      "submit for Simplified declaration" should {
        behave like redirectForDeclarationType(
          DeclarationType.SIMPLIFIED,
          controllers.declaration.routes.RoutingCountriesController.displayRoutingQuestion()
        )
      }

      "submit for Occasional declaration" should {
        behave like redirectForDeclarationType(
          DeclarationType.OCCASIONAL,
          controllers.declaration.routes.RoutingCountriesController.displayRoutingQuestion()
        )
      }

      "submit for Supplementary declaration" should {
        behave like redirectForDeclarationType(DeclarationType.SUPPLEMENTARY, controllers.declaration.routes.LocationController.displayPage())
      }

      "submit for Customs Clearance request" should {
        behave like redirectForDeclarationType(DeclarationType.CLEARANCE, controllers.declaration.routes.LocationController.displayPage())
      }
    }

  }
}
