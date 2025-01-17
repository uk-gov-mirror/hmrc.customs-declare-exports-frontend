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

import controllers.declaration.TotalNumberOfItemsController
import forms.declaration.TotalNumberOfItems
import models.DeclarationType._
import models.Mode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.OptionValues
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerSpec
import views.html.declaration.total_number_of_items

class TotalNumberOfItemsControllerSpec extends ControllerSpec with OptionValues {

  def theResponseForm(mockTotalNumberOfItemsPage: total_number_of_items): Form[TotalNumberOfItems] = {
    val captor = ArgumentCaptor.forClass(classOf[Form[TotalNumberOfItems]])
    verify(mockTotalNumberOfItemsPage).apply(any(), captor.capture())(any(), any())
    captor.getValue
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockTotalNumberOfItemsPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    authorizedUser()
  }

  override protected def afterEach(): Unit = {
    Mockito.reset(mockTotalNumberOfItemsPage)
    super.afterEach()
  }

  override def getFormForDisplayRequest(request: Request[AnyContentAsEmpty.type]): Form[_] = {
    withNewCaching(aDeclaration())
    await(controller.displayPage(Mode.Normal)(request))
    theResponseForm(mockTotalNumberOfItemsPage)
  }

  val mockTotalNumberOfItemsPage: total_number_of_items = mock[total_number_of_items]

  val controller = new TotalNumberOfItemsController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockJourneyAction,
    navigator,
    stubMessagesControllerComponents(),
    mockTotalNumberOfItemsPage,
    mockExportsCacheService
  )(ec)

  val totalNumberOfItems = TotalNumberOfItems(None, None)

  def verifyPage(numberOfTimes: Int = 1) = verify(mockTotalNumberOfItemsPage, times(numberOfTimes)).apply(any(), any())(any(), any())

  "Total Number of Items controller" should {

    onJourney(STANDARD, SUPPLEMENTARY) { request =>
      "display page method is invoked and cache is empty" in {
        withNewCaching(request.cacheModel)
        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe OK
        verifyPage()

        theResponseForm(mockTotalNumberOfItemsPage).value mustBe empty
      }

      "display page method is invoked and cache contains data" in {
        withNewCaching(aDeclaration(withType(request.declarationType), withTotalNumberOfItems(totalNumberOfItems)))

        val result = controller.displayPage(Mode.Normal)(getRequest())

        status(result) mustBe OK
        verifyPage()

        theResponseForm(mockTotalNumberOfItemsPage).value mustNot be(empty)
      }

      "return 400 (BAD_REQUEST) when form is incorrect" in {
        withNewCaching(request.cacheModel)
        val incorrectForm = Json.toJson(TotalNumberOfItems(Some("abc"), None))
        val result = controller.saveNoOfItems(Mode.Normal)(postRequest(incorrectForm))

        status(result) mustBe BAD_REQUEST
        verifyPage()
      }

      "return 303 (SEE_OTHER) when information provided by user are correct" in {
        withNewCaching(request.cacheModel)
        val correctForm = Json.toJson(TotalNumberOfItems(None, None))
        val result = controller.saveNoOfItems(Mode.Normal)(postRequest(correctForm))

        await(result) mustBe aRedirectToTheNextPage
        thePageNavigatedTo mustBe controllers.declaration.routes.TotalPackageQuantityController.displayPage()
        verifyPage(0)
      }

    }

    onJourney(SIMPLIFIED, OCCASIONAL, CLEARANCE) { request =>
      "redirect 303 (See Other) to start" in {
        withNewCaching(request.cacheModel)

        val result = controller.displayPage(Mode.Normal).apply(getRequest(request.cacheModel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) must contain(controllers.routes.RootController.displayPage().url)
      }

    }

  }

}
