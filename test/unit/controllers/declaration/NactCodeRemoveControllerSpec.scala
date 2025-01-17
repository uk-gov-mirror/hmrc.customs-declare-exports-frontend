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

import controllers.declaration.NactCodeRemoveController
import forms.common.YesNoAnswer
import forms.declaration.NactCode
import models.{DeclarationType, Mode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.OptionValues
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.nact_code_remove

class NactCodeRemoveControllerSpec extends ControllerSpec with OptionValues {

  val mockRemovePage = mock[nact_code_remove]

  val controller =
    new NactCodeRemoveController(
      mockAuthAction,
      mockVerifiedEmailAction,
      mockJourneyAction,
      mockExportsCacheService,
      navigator,
      stubMessagesControllerComponents(),
      mockRemovePage
    )(ec)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(mockRemovePage.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockRemovePage)
    super.afterEach()
  }

  private def theResponseForm: Form[YesNoAnswer] = {
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[YesNoAnswer]])
    verify(mockRemovePage).apply(any(), any(), any(), formCaptor.capture())(any(), any())
    formCaptor.getValue
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration())
    await(controller.displayPage(Mode.Normal, item.id, "VATX")(request))
    theResponseForm
  }

  def theNactCode: String = {
    val captor = ArgumentCaptor.forClass(classOf[String])
    verify(mockRemovePage).apply(any(), any(), captor.capture(), any())(any(), any())
    captor.getValue
  }

  private def verifyRemovePageInvoked(numberOfTimes: Int = 1) =
    verify(mockRemovePage, times(numberOfTimes)).apply(any(), any(), any(), any())(any(), any())

  val item = anItem()

  "Nact Code Remove Controller" must {

    onJourney(DeclarationType.STANDARD, DeclarationType.SUPPLEMENTARY, DeclarationType.SIMPLIFIED, DeclarationType.OCCASIONAL) { request =>
      "return 200 (OK)" that {
        "display page method is invoked and cache is empty" in {

          withNewCaching(request.cacheModel)

          val result = controller.displayPage(Mode.Normal, item.id, "VATX")(getRequest())

          status(result) mustBe OK
          verifyRemovePageInvoked()

          theNactCode mustBe "VATX"
        }

      }

      "return 400 (BAD_REQUEST)" when {
        "user submits an invalid answer" in {
          withNewCaching(request.cacheModel)

          val requestBody = Seq("yesNo" -> "invalid")
          val result = controller.submitForm(Mode.Normal, item.id, "VATX")(postRequestAsFormUrlEncoded(requestBody: _*))

          status(result) mustBe BAD_REQUEST
          verifyRemovePageInvoked()
        }

      }
      "return 303 (SEE_OTHER)" when {
        "user submits 'Yes' answer" in {
          val nactCode = NactCode("VATX")
          val item = anItem(withNactCodes(nactCode))
          withNewCaching(aDeclarationAfter(request.cacheModel, withItems(item)))

          val requestBody = Seq("yesNo" -> "Yes")
          val result = controller.submitForm(Mode.Normal, item.id, "VATX")(postRequestAsFormUrlEncoded(requestBody: _*))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.NactCodeSummaryController.displayPage(Mode.Normal, item.id)

          theCacheModelUpdated.itemBy(item.id).flatMap(_.nactCodes) mustBe Some(Seq.empty)
        }

        "user submits 'No' answer" in {
          val nactCode = NactCode("VATX")
          val item = anItem(withNactCodes(nactCode))
          withNewCaching(aDeclarationAfter(request.cacheModel, withItems(item)))

          val requestBody = Seq("yesNo" -> "No")
          val result = controller.submitForm(Mode.Normal, item.id, "VATX")(postRequestAsFormUrlEncoded(requestBody: _*))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.NactCodeSummaryController.displayPage(Mode.Normal, item.id)

          verifyTheCacheIsUnchanged()
        }
      }
    }

    onJourney(DeclarationType.CLEARANCE) { request =>
      "return 303 (SEE_OTHER)" that {
        "display page method is invoked" in {

          withNewCaching(request.cacheModel)

          val result = controller.displayPage(Mode.Normal, item.id, "VATX")(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
        }

        "user submits valid data" in {
          val item = anItem()
          withNewCaching(aDeclarationAfter(request.cacheModel, withItems(item)))

          val requestBody = Seq("yesNo" -> "No")
          val result = controller.submitForm(Mode.Normal, item.id, "VATX")(postRequestAsFormUrlEncoded(requestBody: _*))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RootController.displayPage().url)
        }
      }
    }
  }
}
