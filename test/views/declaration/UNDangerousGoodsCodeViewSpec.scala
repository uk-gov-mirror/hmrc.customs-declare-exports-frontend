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

package views.declaration

import forms.declaration.UNDangerousGoodsCode
import helpers.views.declaration.CommonMessages
import models.DeclarationType.DeclarationType
import models.{DeclarationType, Mode}
import org.jsoup.nodes.Document
import play.api.data.Form
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.declaration.un_dangerous_goods_code
import views.tags.ViewTest

@ViewTest
class UNDangerousGoodsCodeViewSpec extends UnitViewSpec with ExportsTestData with Stubs with CommonMessages {

  private val page = new un_dangerous_goods_code(mainTemplate, minimalAppConfig)
  private val itemId = "item1"
  private val realMessages = validatedMessages
  private def createView(declarationType: DeclarationType = DeclarationType.STANDARD, form: Form[UNDangerousGoodsCode]): Document =
    page(Mode.Normal, itemId, form)(journeyRequest(declarationType), realMessages)

  def uNDangerousGoodsCodeView(declarationType: DeclarationType, dangerousGoodsCode: Option[UNDangerousGoodsCode] = None): Unit = {
    val form = dangerousGoodsCode.fold(UNDangerousGoodsCode.form)(UNDangerousGoodsCode.form.fill(_))
    val view = createView(declarationType, form)

    "display page title" in {

      view.getElementById("title").text() mustBe realMessages("declaration.unDangerousGoodsCode.header")
    }

    "display dangerous goods code input field" in {
      val expectedCode = dangerousGoodsCode.flatMap(_.dangerousGoodsCode).getOrElse("")
      view.getElementById(UNDangerousGoodsCode.dangerousGoodsCodeKey).attr("value") mustBe expectedCode
    }

    "display has dangerous goods code field" in {
      view.getElementById(UNDangerousGoodsCode.hasDangerousGoodsCodeKey).attr("value") mustBe empty
    }

    "display 'Back' button that links to 'Package Information' page" in {
      val backButton = view.getElementById("link-back")

      backButton.getElementById("link-back") must haveHref(controllers.declaration.routes.CommodityDetailsController.displayPage(Mode.Normal, itemId))
    }

    "display 'Save and continue' button on page" in {

      val saveButton = view.select("#submit")
      saveButton.text() mustBe realMessages(saveAndContinueCaption)
    }
  }

  "UN Dangerous Goods Code View on empty page" when {
    "we are on Standard journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.STANDARD)
    }
    "we are on Supplementary journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.SUPPLEMENTARY)
    }
    "we are on Simplified journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.SIMPLIFIED)
    }
  }

  "UN Dangerous Goods Code View on populated page" when {
    val dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("1234")))

    "we are on Standard journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.STANDARD, dangerousGoodsCode)
    }
    "we are on Supplementary journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.SUPPLEMENTARY, dangerousGoodsCode)
    }
    "we are on Simplified journey" should {
      behave like uNDangerousGoodsCodeView(DeclarationType.SIMPLIFIED, dangerousGoodsCode)
    }
  }
}