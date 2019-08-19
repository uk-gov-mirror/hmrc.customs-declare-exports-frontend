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

package unit.controllers.declaration

import controllers.declaration.ProcedureCodesController
import controllers.util.Remove
import forms.Choice.AllowedChoiceValues.SupplementaryDec
import forms.declaration.ProcedureCodes
import models.declaration.ProcedureCodesData
import models.declaration.ProcedureCodesData.limitOfCodes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.OptionValues
import play.api.data.Form
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.cache.ExportItem
import unit.base.ControllerSpec
import unit.mock.ErrorHandlerMocks
import views.html.declaration.procedure_codes

class ProcedureCodesControllerSpec extends ControllerSpec with ErrorHandlerMocks with OptionValues {

  val mockProcedureCodesPage = mock[procedure_codes]

  val controller = new ProcedureCodesController(
    mockAuthAction,
    mockJourneyAction,
    mockErrorHandler,
    mockExportsCacheService,
    stubMessagesControllerComponents(),
    mockProcedureCodesPage
  )(ec)

  val itemId = "itemId12345"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    setupErrorHandler()
    when(mockProcedureCodesPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockProcedureCodesPage)
    super.afterEach()
  }

  def theResponse: (Form[ProcedureCodes], Seq[String]) = {
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[ProcedureCodes]])
    val dataCaptor = ArgumentCaptor.forClass(classOf[Seq[String]])
    verify(mockProcedureCodesPage).apply(any(), formCaptor.capture(), dataCaptor.capture())(any(), any())
    (formCaptor.getValue, dataCaptor.getValue)
  }

  "Procedure Codes controller" should {

    "return 200 (OK)" when {

      "display page method is invoked with empty cache" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val result = controller.displayPage(itemId)(getRequest())

        status(result) mustBe OK
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())

        val (responseForm, responseSeq) = theResponse
        responseForm.value mustBe empty
        responseSeq mustBe empty
      }

      "display page method is invoked with data in cache" in {

        val item = ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val result = controller.displayPage(itemId)(getRequest())

        status(result) mustBe OK
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())

        val (responseForm, responseSeq) = theResponse
        responseForm.value.value.procedureCode.value mustBe "1234"
        responseSeq mustBe Seq("123")
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user provide wrong action" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val wrongAction = Seq(("procedureCode", "1234"), ("additionalProcedureCode", "123"), ("WrongAction", ""))

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(wrongAction: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }
    }

    "return 400 (BAD_REQUEST) during adding" when {

      "user put incorrect data" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val incorrectForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "incorrect"), addActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(incorrectForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }

      "user put duplicated item" in {

        val item = ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val duplicatedForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "123"), addActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(duplicatedForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }

      "user reach maximum amount of items" in {

        val item =
          ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq.fill(limitOfCodes)("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val correctForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "321"), addActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(correctForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }
    }

    "return 400 (BAD_REQUEST) during saving" when {

      "user put incorrect data" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val incorrectForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "incorrect"), saveAndContinueActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(incorrectForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }

      "user put duplicated item" in {

        val item = ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val duplicatedForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "123"), saveAndContinueActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(duplicatedForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }

      "user reach maximum amount of items" in {

        val item =
          ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq.fill(limitOfCodes)("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val correctForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "321"), saveAndContinueActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(correctForm: _*))

        status(result) mustBe BAD_REQUEST
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }
    }

    "return 303 (SEE_OTHER)" when {

      "user correctly add new item" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val correctForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "321"), addActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        verify(mockProcedureCodesPage, times(0)).apply(any(), any(), any())(any(), any())
      }

      "user save correct data" in {

        withNewCaching(aDeclaration(withChoice(SupplementaryDec)))

        val correctForm =
          Seq(("procedureCode", "1234"), ("additionalProcedureCode", "321"), saveAndContinueActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        verify(mockProcedureCodesPage, times(0)).apply(any(), any(), any())(any(), any())
      }

      "user save correct data without new item" in {

        val item =
          ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val correctForm =
          Seq(("procedureCode", "1234"), saveAndContinueActionUrlEncoded)

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        verify(mockProcedureCodesPage, times(0)).apply(any(), any(), any())(any(), any())
      }

      "user remove existing item" in {

        val item =
          ExportItem(itemId, procedureCodes = Some(ProcedureCodesData(Some("1234"), Seq("123"))))
        withNewCaching(aDeclaration(withItem(item)))

        val removeAction = (Remove.toString, "123")

        val result = controller.submitProcedureCodes(itemId)(postRequestAsFormUrlEncoded(removeAction))

        status(result) mustBe OK
        verify(mockProcedureCodesPage, times(1)).apply(any(), any(), any())(any(), any())
      }
    }
  }
}