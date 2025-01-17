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

import controllers.declaration.PersonPresentingGoodsDetailsController
import forms.common.Eori
import forms.declaration.PersonPresentingGoodsDetails
import models.DeclarationType.{OCCASIONAL, SIMPLIFIED, STANDARD, SUPPLEMENTARY}
import models.{DeclarationType, ExportsDeclaration, Mode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.person_presenting_goods_details

class PersonPresentingGoodsDetailsControllerSpec extends ControllerSpec with ScalaFutures {

  private val testEori = "GB1234567890000"

  private val page = mock[person_presenting_goods_details]

  private val controller = new PersonPresentingGoodsDetailsController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    mockExportsCacheService,
    navigator,
    stubMessagesControllerComponents(),
    page
  )(ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration(withType(DeclarationType.CLEARANCE)))
    await(controller.displayPage(Mode.Normal)(request))
    theFormPassedToView
  }

  def theFormPassedToView: Form[PersonPresentingGoodsDetails] = {
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[PersonPresentingGoodsDetails]])
    verify(page).apply(any(), formCaptor.capture())(any(), any())
    formCaptor.getValue
  }

  def theModelPassedToCacheUpdate: ExportsDeclaration = {
    val modelCaptor = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(mockExportsCacheService).update(modelCaptor.capture())(any())
    modelCaptor.getValue
  }

  "PersonPresentingGoodsDetailsController on displayPage" when {

    onClearance { request =>
      "everything works correctly" should {

        "return 200 (OK)" in {

          withNewCaching(request.cacheModel)

          val result = controller.displayPage(Mode.Normal)(getRequest())

          status(result) mustBe OK
        }

        "call ExportsCacheService" in {

          withNewCaching(request.cacheModel)

          controller.displayPage(Mode.Normal)(getRequest()).futureValue

          verify(mockExportsCacheService).get(meq("declarationId"))(any())
        }

        "call page view, passing form with data from cache" in {

          withNewCaching(aDeclarationAfter(request.cacheModel, withPersonPresentingGoodsDetails(Some(Eori(testEori)))))

          controller.displayPage(Mode.Normal)(getRequest()).futureValue

          theFormPassedToView.value mustBe defined
          theFormPassedToView.value.map(_.eori) mustBe Some(Eori(testEori))
        }
      }
    }

    onJourney(STANDARD, SUPPLEMENTARY, SIMPLIFIED, OCCASIONAL) { request =>
      "return 303 (SEE_OTHER)" in {

        withNewCaching(request.cacheModel)

        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe SEE_OTHER
      }

      "redirect to start page" in {

        withNewCaching(request.cacheModel)

        val result = controller.displayPage(Mode.Normal)(getRequest())

        redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
      }
    }
  }

  "PersonPresentingGoodsDetailsController on submitForm" when {

    onClearance { request =>
      "everything works correctly" should {

        "return 303 (SEE_OTHER)" in {

          withNewCaching(request.cacheModel)
          val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

          val result = controller.submitForm(Mode.Normal)(postRequest(correctForm))

          status(result) mustBe SEE_OTHER
        }

        "redirect to Exporter Details page" in {

          withNewCaching(request.cacheModel)
          val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

          controller.submitForm(Mode.Normal)(postRequest(correctForm)).futureValue

          thePageNavigatedTo mustBe controllers.declaration.routes.ExporterEoriNumberController.displayPage()
        }

        "call Cache to update it" in {

          withNewCaching(request.cacheModel)
          val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

          controller.submitForm(Mode.Normal)(postRequest(correctForm)).futureValue

          theModelPassedToCacheUpdate.parties.personPresentingGoodsDetails mustBe Some(PersonPresentingGoodsDetails(Eori(testEori)))
        }

        "call Navigator" in {

          withNewCaching(request.cacheModel)
          val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

          controller.submitForm(Mode.Normal)(postRequest(correctForm)).futureValue

          verify(navigator).continueTo(any(), any(), any())(any(), any())
        }
      }

      "provided with incorrect data" should {
        "return 400 (BAD_REQUEST)" in {

          withNewCaching(request.cacheModel)
          val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> "Incorrect!@#"))

          val result = controller.submitForm(Mode.Normal)(postRequest(correctForm))

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    onJourney(STANDARD, SUPPLEMENTARY, SIMPLIFIED, OCCASIONAL) { request =>
      "return 303 (SEE_OTHER)" in {

        withNewCaching(request.cacheModel)
        val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

        val result = controller.submitForm(Mode.Normal)(postRequest(correctForm))

        status(result) mustBe SEE_OTHER
      }

      "redirect to start page" in {

        withNewCaching(request.cacheModel)
        val correctForm = Json.toJson(Map(PersonPresentingGoodsDetails.fieldName -> testEori))

        val result = controller.submitForm(Mode.Normal)(postRequest(correctForm))

        redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
      }
    }
  }

}
