/*
 * Copyright 2020 HM Revenue & Customs
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

package views.declaration.summary

import forms.declaration.additionaldocuments.DocumentsProduced
import models.Mode
import services.cache.ExportsTestData
import views.declaration.spec.UnitViewSpec
import views.html.declaration.summary.supporting_documents

class SupportingDocumentsViewSpec extends UnitViewSpec with ExportsTestData {

  "Supporting documents view" should {

    "display title only and change link" when {

      "there is no documents" in {

        val view = supporting_documents(Mode.Normal, "itemId", 1, Seq.empty)(messages, journeyRequest())

        view.getElementById("supporting-documents-1-label").text() mustBe messages("declaration.summary.items.item.supportingDocuments")
        view.getElementById("supporting-documents-1-change") must haveHref(
          controllers.declaration.routes.DocumentsProducedController.displayPage(Mode.Normal, "itemId")
        )
      }
    }

    "display all supporting documents with change buttons" in {

      val documents = Seq(
        DocumentsProduced(Some("typ1"), Some("identifier1"), None, None, None, None, None),
        DocumentsProduced(Some("typ2"), Some("identifier2"), None, None, None, None, None)
      )
      val view = supporting_documents(Mode.Normal, "itemId", 1, documents)(messages, journeyRequest())

      view.getElementById("supporting-documents-1").text() mustBe messages("declaration.summary.items.item.supportingDocuments")
      view.getElementById("supporting-documents-code-1").text() mustBe messages("declaration.summary.items.item.supportingDocuments.code")
      view.getElementById("supporting-documents-information-1").text() mustBe messages(
        "declaration.summary.items.item.supportingDocuments.information"
      )
      view.getElementById("supporting-document-1-code-0").text() mustBe "typ1"
      view.getElementById("supporting-document-1-information-0").text() mustBe "identifier1"

      val List(change1, accessibleChange1) = view.getElementById("supporting-document-1-change-0").text().split(" ").toList

      change1 mustBe messages("site.change")
      accessibleChange1 mustBe messages("declaration.summary.items.item.supportingDocuments.change", 1)

      view.getElementById("supporting-document-1-change-0") must haveHref(
        controllers.declaration.routes.DocumentsProducedController.displayPage(Mode.Normal, "itemId")
      )
      view.getElementById("supporting-document-1-code-1").text() mustBe "typ2"
      view.getElementById("supporting-document-1-information-1").text() mustBe "identifier2"

      val List(change2, accessibleChange2) = view.getElementById("supporting-document-1-change-1").text().split(" ").toList

      change2 mustBe messages("site.change")
      accessibleChange2 mustBe messages("declaration.summary.items.item.supportingDocuments.change", 2)

      view.getElementById("supporting-document-1-change-1") must haveHref(
        controllers.declaration.routes.DocumentsProducedController.displayPage(Mode.Normal, "itemId")
      )
    }
  }
}
