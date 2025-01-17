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

import controllers.declaration.AdditionalInformationRequiredController
import forms.common.YesNoAnswer
import models.Mode
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.OptionValues
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.additionalInformation.additional_information_required

class AdditionalInformationRequiredControllerSpec extends ControllerSpec with OptionValues {

  val mockPage = mock[additional_information_required]

  val controller = new AdditionalInformationRequiredController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    mockExportsCacheService,
    navigator,
    stubMessagesControllerComponents(),
    mockPage
  )(ec)

  val itemId = "itemId"

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration(withItem(anItem(withItemId(itemId)))))
    await(controller.displayPage(Mode.Normal, itemId)(request))
    theResponseForm
  }

  def theResponseForm: Form[YesNoAnswer] = {
    val captor = ArgumentCaptor.forClass(classOf[Form[YesNoAnswer]])
    verify(mockPage).apply(any(), any(), captor.capture())(any(), any())
    captor.getValue
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(mockPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockPage)
    super.afterEach()
  }

  private def verifyPageInvoked(numberOfTimes: Int = 1) = verify(mockPage, times(numberOfTimes)).apply(any(), any(), any())(any(), any())

  "AdditionalInformationRequired Controller" should {

    onEveryDeclarationJourney() { request =>
      "return 200 (OK)" that {

        "display page method is invoked and cache is empty" in {
          withNewCaching(aDeclarationAfter(request.cacheModel, withItem(anItem(withItemId(itemId)))))

          val result = controller.displayPage(Mode.Normal, itemId)(getRequest())

          status(result) mustBe OK
          verifyPageInvoked()
        }
      }

      "return 400 (BAD_REQUEST)" when {

        "user submits invalid answer" in {
          withNewCaching(aDeclarationAfter(request.cacheModel, withItem(anItem(withItemId(itemId)))))

          val requestBody = Seq("yesNo" -> "invalid")
          val result = controller.submitForm(Mode.Normal, itemId)(postRequestAsFormUrlEncoded(requestBody: _*))

          status(result) mustBe BAD_REQUEST
          verifyPageInvoked()
        }

      }

      "return 303 (SEE_OTHER)" when {

        "Additional item(s) exist in cache" in {
          withNewCaching(
            aDeclarationAfter(request.cacheModel, withItem(anItem(withItemId(itemId), withAdditionalInformation("code", "description"))))
          )

          val result = controller.displayPage(Mode.Normal, itemId)(getRequest())

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.AdditionalInformationController.displayPage(Mode.Normal, itemId)
        }

        "user submits valid Yes answer" in {
          withNewCaching(aDeclarationAfter(request.cacheModel, withItem(anItem(withItemId(itemId)))))

          val requestBody = Seq("yesNo" -> "Yes")
          val result = controller.submitForm(Mode.Normal, itemId)(postRequestAsFormUrlEncoded(requestBody: _*))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.AdditionalInformationController.displayPage(Mode.Normal, itemId)
        }

        "user submits valid No answer" in {
          withNewCaching(aDeclarationAfter(request.cacheModel, withItem(anItem(withItemId(itemId)))))

          val requestBody = Seq("yesNo" -> "No")
          val result = controller.submitForm(Mode.Normal, itemId)(postRequestAsFormUrlEncoded(requestBody: _*))

          await(result) mustBe aRedirectToTheNextPage
          thePageNavigatedTo mustBe controllers.declaration.routes.DocumentsProducedController.displayPage(Mode.Normal, itemId)
        }
      }
    }

  }
}
