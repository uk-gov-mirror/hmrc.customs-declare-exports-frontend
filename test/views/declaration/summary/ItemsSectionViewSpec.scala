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

package views.declaration.summary

import base.Injector
import models.Mode
import services.cache.ExportsTestData
import views.declaration.spec.UnitViewSpec
import views.html.declaration.summary.items_section

class ItemsSectionViewSpec extends UnitViewSpec with ExportsTestData with Injector {

  "Items section" should {

    val itemsSection = instanceOf[items_section]

    "display nothing" when {

      "there is no items in the declaration" in {

        val view = itemsSection(Mode.Normal, aDeclaration())(messages, journeyRequest())

        view.getAllElements.text() must be(empty)
      }
    }

    "display items" when {

      "item exists" in {

        val data =
          aDeclaration(withItems(anItem(withSequenceId(1), withStatisticalValue("10")), anItem(withSequenceId(2), withProcedureCodes(Some("code")))))

        val view = itemsSection(Mode.Normal, data)(messages, journeyRequest())

        view.getElementById("declaration-items-summary-1").text() mustNot be(empty)
        view.getElementById("declaration-items-summary-2").text() mustNot be(empty)
      }
    }
  }
}
