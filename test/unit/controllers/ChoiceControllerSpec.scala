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

package unit.controllers

import base.ExportsTestData._
import config.{AppConfig, SecureMessagingInboxConfig}
import controllers.ChoiceController
import forms.Choice
import forms.Choice.AllowedChoiceValues._
import models.DeclarationType
import models.DeclarationType.DeclarationType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.OptionValues
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerWithoutFormSpec
import utils.FakeRequestCSRFSupport._
import views.html.choice_page

class ChoiceControllerSpec extends ControllerWithoutFormSpec with OptionValues {
  import ChoiceControllerSpec._

  val choicePage = mock[choice_page]
  val appConfig = mock[AppConfig]
  val secureMessagingInboxConfig = mock[SecureMessagingInboxConfig]

  val controller =
    new ChoiceController(
      mockAuthAction,
      mockVerifiedEmailAction,
      stubMessagesControllerComponents(),
      secureMessagingInboxConfig,
      choicePage,
      appConfig
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(choicePage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(appConfig.availableJourneys()).thenReturn(allJourneys)
  }

  override protected def afterEach(): Unit = {
    reset(choicePage, appConfig, secureMessagingInboxConfig)
    super.afterEach()
  }

  def postChoiceRequest(body: JsValue): Request[AnyContentAsJson] =
    FakeRequest("POST", "")
      .withJsonBody(body)
      .withCSRFToken

  private def existingDeclaration(choice: DeclarationType = DeclarationType.SUPPLEMENTARY) =
    aDeclaration(withId("existingDeclarationId"), withType(choice))

  "ChoiceController displayPage" should {

    "return 200 (OK)" when {

      "display page method is invoked with empty cache" in {
        withNoDeclaration()

        val result = controller.displayPage(None)(getRequest())

        status(result) must be(OK)
      }

      "display page method is invoked with data in cache" in {
        withNewCaching(existingDeclaration())

        val result = controller.displayPage(None)(getRequest())

        status(result) must be(OK)
      }
    }

    "pre-select given choice " when {

      "cache is empty" in {
        withNoDeclaration()

        val request = getRequest()
        val result = controller.displayPage(Some(Choice(CancelDec)))(request)
        val form = Choice.form().fill(Choice(CancelDec))

        viewOf(result) must be(choicePage(form, allJourneys)(request, controller.messagesApi.preferred(request)))
      }

      "cache contains existing declaration" in {
        withNewCaching(existingDeclaration())

        val request = getRequest()
        val result = controller.displayPage(Some(Choice(Submissions)))(request)
        val form = Choice.form().fill(Choice(Submissions))

        viewOf(result) must be(choicePage(form, allJourneys)(request, controller.messagesApi.preferred(request)))
      }
    }

    "not select any choice " when {

      "choice parameter not given and cache empty" in {
        withNoDeclaration()

        val request = getRequest()
        val result = controller.displayPage(None)(request)
        val form = Choice.form()

        viewOf(result) must be(choicePage(form, allJourneys)(request, controller.messagesApi.preferred(request)))
      }
    }
  }

  "ChoiceController submitChoice" should {

    "return 400 (BAD_REQUEST)" when {
      "form is incorrect" in {
        val result = controller.submitChoice()(postChoiceRequest(incorrectChoice))

        status(result) must be(BAD_REQUEST)
        verifyTheCacheIsUnchanged()
      }
    }

    "redirect to Declaration choice page" when {
      "user chooses Create Dec " in {
        val result = controller.submitChoice()(postChoiceRequest(createChoice))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.declaration.routes.DeclarationChoiceController.displayPage().url))
        verifyTheCacheIsUnchanged()
      }
    }

    "redirect to Cancel Declaration page" when {
      "user chose Cancel Dec" in {
        val result = controller.submitChoice()(postChoiceRequest(cancelChoice))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.CancelDeclarationController.displayPage().url))
        verifyTheCacheIsUnchanged()
      }
    }

    "redirect to Submissions page" when {
      "user chose submissions" in {
        val result = controller.submitChoice()(postChoiceRequest(submissionsChoice))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.SubmissionsController.displayListOfSubmissions().url))
        verifyTheCacheIsUnchanged()
      }
    }

    "redirect to Saved Declarations page" when {
      "user chose continue a saved declaration" in {
        val result = controller.submitChoice()(postChoiceRequest(continueDeclarationChoice))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.SavedDeclarationsController.displayDeclarations().url))
        verifyTheCacheIsUnchanged()
      }
    }

    "redirect to Exports secure messaging inbox page" when {
      "user chose view messages" in {
        val result = controller.submitChoice()(postChoiceRequest(inboxChoice))

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.SecureMessagingController.displayInbox().url))
        verifyTheCacheIsUnchanged()
      }
    }
  }

  "ChoiceController availableJourneys" should {
    "contain all journey types when isExportsSecureMessagingEnabled returns true" in {
      when(secureMessagingInboxConfig.isExportsSecureMessagingEnabled).thenReturn(true)

      val choiceCtrl = new ChoiceController(
        mockAuthAction,
        mockVerifiedEmailAction,
        stubMessagesControllerComponents(),
        secureMessagingInboxConfig,
        choicePage,
        appConfig
      )
      allJourneys.diff(choiceCtrl.availableJourneys).size mustBe 0
    }

    "contain all journey types apart from 'Inbox' when isExportsSecureMessagingEnabled returns false" in {
      when(secureMessagingInboxConfig.isExportsSecureMessagingEnabled).thenReturn(false)

      val choiceCtrl = new ChoiceController(
        mockAuthAction,
        mockVerifiedEmailAction,
        stubMessagesControllerComponents(),
        secureMessagingInboxConfig,
        choicePage,
        appConfig
      )
      val missingJourneyTypes = allJourneys.diff(choiceCtrl.availableJourneys)
      missingJourneyTypes.size mustBe 1
      missingJourneyTypes must contain(Inbox)
    }
  }
}

object ChoiceControllerSpec {
  val incorrectChoice: JsValue = Json.toJson(Choice("Incorrect Choice"))
  val createChoice: JsValue = Json.toJson(Choice(CreateDec))
  val cancelChoice: JsValue = Json.toJson(Choice(CancelDec))
  val submissionsChoice: JsValue = Json.toJson(Choice(Submissions))
  val continueDeclarationChoice: JsValue = Json.toJson(Choice(ContinueDec))
  val inboxChoice: JsValue = Json.toJson(Choice(Inbox))
}
