/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import api.declaration.{Declarant, Declaration, SubmitDeclaration}
import utils.FakeNavigator
import connectors.FakeDataCacheConnector
import controllers.actions._
import play.api.test.Helpers._
import models.NormalMode
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.Future

class DeclarationSummaryControllerSpec extends ControllerSpecBase with MockitoSugar {

  trait Scope {
    val mockFakeNavigator = mock[FakeNavigator]
    val mockSubmitDeclaration = mock[SubmitDeclaration]

    def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
      new DeclarationSummaryController(
        frontendAppConfig,
        messagesApi,
        FakeDataCacheConnector,
        mockFakeNavigator,
        FakeAuthAction,
        dataRetrievalAction,
        new DataRequiredActionImpl,
        mockSubmitDeclaration
      )
  }

  "Declaration summary controller" must {
    "return OK and the correct view for a GET" in new Scope {
      val result = controller().onPageLoad(NormalMode)(fakeRequest)

      status(result) mustBe OK
    }

    "return OK after send declaration" in new Scope {
      val result = controller().onSubmit(NormalMode)(fakeRequest)
      val declaration = new Declaration(new Declarant("123"))

      when(mockSubmitDeclaration.submit(declaration, "Non CSP"))
        .thenReturn(Future.successful(1))

      status(result) mustBe OK
    }
  }
}
