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

import controllers.declaration._
import forms.declaration.WarehouseIdentification
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
import views.html.declaration.{warehouse_identification, warehouse_identification_yesno}

class WarehouseIdentificationControllerSpec extends ControllerSpec {

  private val pageYesNo = mock[warehouse_identification_yesno]
  private val pageIdentification = mock[warehouse_identification]

  val controller = new WarehouseIdentificationController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    navigator,
    mockExportsCacheService,
    stubMessagesControllerComponents(),
    pageYesNo,
    pageIdentification
  )(ec)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    withNewCaching(aDeclaration(withType(DeclarationType.STANDARD)))
    when(pageYesNo.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(pageIdentification.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(pageYesNo, pageIdentification)
  }

  def theResponseForm: Form[WarehouseIdentification] = {
    val captor = ArgumentCaptor.forClass(classOf[Form[WarehouseIdentification]])
    verify(pageIdentification).apply(any(), captor.capture())(any(), any())
    captor.getValue
  }

  def theResponseFormYesNo: Form[WarehouseIdentification] = {
    val captor = ArgumentCaptor.forClass(classOf[Form[WarehouseIdentification]])
    verify(pageYesNo).apply(any(), captor.capture())(any(), any())
    captor.getValue
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration())
    await(controller.displayPage(Mode.Normal)(request))
    theResponseForm
  }

  private val identification = WarehouseIdentification(Some("R12345678GB"))

  "Warehouse Identification Controller" should {

    onJourney(STANDARD, SIMPLIFIED, OCCASIONAL, SUPPLEMENTARY) { request =>
      "return 200 (OK)" when {

        "display page method is invoked and cache is empty" in {

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
        }

        "display page method is invoked and cache is not empty" in {

          withNewCaching(aDeclarationAfter(request.cacheModel, withWarehouseIdentification(Some(identification))))

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
          theResponseForm.value mustBe Some(identification)
        }
      }

      "return 400 (BAD_REQUEST)" when {

        "form contains incorrect values" in {

          withNewCaching(request.cacheModel)

          val incorrectForm = Json.obj(WarehouseIdentification.warehouseIdKey -> "incorrect")

          val result = controller.saveIdentificationNumber(Mode.Normal)(postRequest(incorrectForm))

          status(result) must be(BAD_REQUEST)
        }
      }

      "return 303 (SEE_OTHER)" when {

        "form contains valid response" in {
          withNewCaching(request.cacheModel)
          val correctForm =
            Json.obj(WarehouseIdentification.warehouseIdKey -> "R12341234")

          val result = controller.saveIdentificationNumber(Mode.Normal)(postRequest(correctForm))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe routes.SupervisingCustomsOfficeController.displayPage()
        }

      }

    }

    onJourney(CLEARANCE) { request =>
      "return 200 (OK)" when {

        "display page method is invoked and cache is empty" in {

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
        }

        "display page method is invoked and cache is not empty" in {

          withNewCaching(aDeclarationAfter(request.cacheModel, withWarehouseIdentification(Some(identification))))

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) must be(OK)
          theResponseFormYesNo.value mustBe Some(identification)
        }
      }

      "return 400 (BAD_REQUEST)" when {

        "form contains incorrect values" in {

          withNewCaching(request.cacheModel)

          val incorrectForm = Json.obj(WarehouseIdentification.inWarehouseKey -> "Yes", WarehouseIdentification.warehouseIdKey -> "incorrect")

          val result = controller.saveIdentificationNumber(Mode.Normal)(postRequest(incorrectForm))

          status(result) must be(BAD_REQUEST)
        }
      }

      "return 303 (SEE_OTHER)" when {

        "form contains valid response" in {

          withNewCaching(request.cacheModel)

          val correctForm = Json.obj(WarehouseIdentification.inWarehouseKey -> "Yes", WarehouseIdentification.warehouseIdKey -> "R12341234")

          val result = controller.saveIdentificationNumber(Mode.Normal)(postRequest(correctForm))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe routes.SupervisingCustomsOfficeController.displayPage()
        }

      }
    }

  }
}
