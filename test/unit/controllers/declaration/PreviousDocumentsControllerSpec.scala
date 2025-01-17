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

import controllers.declaration.PreviousDocumentsController
import forms.declaration.{Document, DocumentSpec, PreviousDocumentsData}
import models.declaration.DocumentCategory.SimplifiedDeclaration
import models.{DeclarationType, Mode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import unit.base.ControllerWithoutFormSpec
import views.html.declaration.previousDocuments.previous_documents

class PreviousDocumentsControllerSpec extends ControllerWithoutFormSpec {

  val mockPreviousDocumentsPage = mock[previous_documents]

  val controller = new PreviousDocumentsController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    navigator,
    stubMessagesControllerComponents(),
    mockPreviousDocumentsPage,
    mockExportsCacheService
  )(ec)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    authorizedUser()
    withNewCaching(aDeclaration(withType(DeclarationType.SUPPLEMENTARY)))
    when(mockPreviousDocumentsPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockPreviousDocumentsPage)

    super.afterEach()
  }

  def theResponse: Form[Document] = {
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Document]])
    verify(mockPreviousDocumentsPage).apply(any(), formCaptor.capture())(any(), any())
    formCaptor.getValue
  }

  def verifyPage(numberOfTimes: Int = 1): Html =
    verify(mockPreviousDocumentsPage, times(numberOfTimes)).apply(any(), any())(any(), any())

  "Previous Documents controller" should {

    "return 200 (OK)" when {

      "display page method " in {

        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe OK
        verifyPage()

        theResponse.value mustBe empty
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user put incorrect data" in {

        val incorrectForm = DocumentSpec.json("355", "reference", "incorrect", "")

        val result = controller.savePreviousDocuments(Mode.Normal)(postRequest(incorrectForm))

        status(result) mustBe BAD_REQUEST
        verifyPage()
      }

      "user put duplicated item" in {

        val document = Document("355", "reference", SimplifiedDeclaration, Some("123"))
        withNewCaching(aDeclaration(withPreviousDocuments(document)))

        val duplicatedForm = Json.toJson(Document("355", "reference", SimplifiedDeclaration, Some("123")))

        val result = controller.savePreviousDocuments(Mode.Normal)(postRequest(duplicatedForm))

        status(result) mustBe BAD_REQUEST
        verifyPage()
      }

      "user reach maximum amount of items" in {

        val document = Document("355", "reference", SimplifiedDeclaration, Some("123"))
        withNewCaching(aDeclaration(withPreviousDocuments(PreviousDocumentsData(Seq.fill(PreviousDocumentsData.maxAmountOfItems)(document)))))

        val correctForm = Json.toJson(Document("355", "reference", SimplifiedDeclaration, None))

        val result = controller.savePreviousDocuments(Mode.Normal)(postRequest(correctForm))

        status(result) mustBe BAD_REQUEST
        verifyPage()
      }
    }

    "return 303 (SEE_OTHER)" when {

      "user put correct data" in {

        val correctForm = Json.toJson(Document("355", "reference", SimplifiedDeclaration, None))

        val result = controller.savePreviousDocuments(Mode.Normal)(postRequest(correctForm))

        await(result) mustBe aRedirectToTheNextPage
        thePageNavigatedTo mustBe controllers.declaration.routes.PreviousDocumentsSummaryController.displayPage()

        verifyPage(0)
      }

      "user doesn't provide any information and it's first document" in {

        withNewCaching(aDeclaration(withoutPreviousDocuments()))

        val emptyForm = DocumentSpec.json("", "", "", "")
        ()
        val result = controller.savePreviousDocuments(Mode.Normal)(postRequest(emptyForm))

        await(result) mustBe aRedirectToTheNextPage
        thePageNavigatedTo mustBe controllers.declaration.routes.ItemsSummaryController.displayAddItemPage()

        verifyPage(0)
      }
    }
  }
}
