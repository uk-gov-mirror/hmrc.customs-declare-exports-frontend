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

import controllers.declaration.ConsignorDetailsController
import forms.common.{Address, Eori}
import forms.declaration.EntityDetails
import forms.declaration.consignor.ConsignorDetails
import models.DeclarationType._
import models.{DeclarationType, Mode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.consignor_details

class ConsignorDetailsControllerSpec extends ControllerSpec {

  val consignorDetailsPage = mock[consignor_details]

  val controller = new ConsignorDetailsController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    mockExportsCacheService,
    navigator,
    stubMessagesControllerComponents(),
    consignorDetailsPage
  )(ec)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    authorizedUser()
    when(consignorDetailsPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(consignorDetailsPage)

    super.afterEach()
  }

  def theResponseForm: Form[ConsignorDetails] = {
    val captor = ArgumentCaptor.forClass(classOf[Form[ConsignorDetails]])
    verify(consignorDetailsPage).apply(any(), captor.capture())(any(), any())
    captor.getValue
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration(withType(DeclarationType.CLEARANCE)))
    await(controller.displayPage(Mode.Normal)(request))
    theResponseForm
  }

  "Consignor Details controller" should {

    onJourney(CLEARANCE) { request =>
      "return 200 (OK)" when {

        "display page method is invoked and cache is empty" in {

          withNewCaching(request.cacheModel)

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
        }

        "display page method is invoked and cache contains data" in {

          withNewCaching(
            aDeclarationAfter(
              request.cacheModel,
              withConsignorDetails(
                None,
                Some(Address("John Smith", "1 Export Street", "Leeds", "LS1 2PW", "United Kingdom, Great Britain, Northern Ireland"))
              )
            )
          )

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
        }
      }

      "return 400 (BAD_REQUEST)" when {

        "form is incorrect" in {

          withNewCaching(request.cacheModel)

          val incorrectForm = Json.toJson(ConsignorDetails(EntityDetails(None, None)))

          val result = controller.saveAddress(Mode.Normal)(postRequest(incorrectForm))

          status(result) must be(BAD_REQUEST)
        }
      }
    }

    onJourney(CLEARANCE) { request =>
      "return 303 (SEE_OTHER) and redirect to representative details page" when {

        "form is correct" in {

          withNewCaching(request.cacheModel)

          val correctForm =
            Json.toJson(
              ConsignorDetails(
                EntityDetails(
                  None,
                  Some(Address("John Smith", "1 Export Street", "Leeds", "LS1 2PW", "United Kingdom, Great Britain, Northern Ireland"))
                )
              )
            )

          val result = controller.saveAddress(Mode.Normal)(postRequest(correctForm))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.RepresentativeAgentController.displayPage()
        }
      }
      "return 303 (SEE_OTHER) and redirect to carrier details page" when {

        "form is correct" in {

          withNewCaching(
            aDeclaration(
              withType(DeclarationType.CLEARANCE),
              withDeclarantIsExporter(),
              withDeclarantDetails(eori = Some(Eori("GB12345678"))),
              withExporterDetails(eori = Some(Eori("GB12345678")))
            )
          )

          val correctForm =
            Json.toJson(
              ConsignorDetails(
                EntityDetails(
                  None,
                  Some(Address("John Smith", "1 Export Street", "Leeds", "LS1 2PW", "United Kingdom, Great Britain, Northern Ireland"))
                )
              )
            )

          val result = controller.saveAddress(Mode.Normal)(postRequest(correctForm))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.CarrierEoriNumberController.displayPage()
        }
      }
    }

    onJourney(STANDARD, SUPPLEMENTARY, OCCASIONAL, SIMPLIFIED) { request =>
      "redirect to start" in {
        withNewCaching(request.cacheModel)

        val correctForm =
          Json.toJson(
            ConsignorDetails(
              EntityDetails(
                None,
                Some(Address("John Smith", "1 Export Street", "Leeds", "LS1 2PW", "United Kingdom, Great Britain, Northern Ireland"))
              )
            )
          )

        val result = controller.saveAddress(Mode.Normal)(postRequest(correctForm))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
      }
    }

    onJourney(STANDARD, SUPPLEMENTARY, OCCASIONAL, SIMPLIFIED) { request =>
      "return 200 (OK)" when {

        "display page method is invoked and cache is empty" in {

          withNewCaching(request.cacheModel)

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
        }
      }
    }
  }
}
