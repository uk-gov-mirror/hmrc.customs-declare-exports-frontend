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

package views.declaration.summary

import forms.declaration.GoodsLocation
import services.cache.ExportsTestData
import views.declaration.spec.UnitViewSpec
import views.html.declaration.summary.locations_section

class LocationsSectionViewSpec extends UnitViewSpec with ExportsTestData {

  val data = aDeclaration(
    withGoodsLocation(GoodsLocation("United Kingdom", "A", "U", Some("123"), None, Some("addressLine"), Some("postCode"), Some("city"))),
    withOfficeOfExit("123", Some("12"))
  )
  val view = locations_section(data)(messages, journeyRequest())

  "Locations section" should {

    "have a goods location code" in {

      view.getElementById("location-code-label").text() mustBe messages("declaration.summary.locations.goodsLocationCode")
      view.getElementById("location-code").text() mustBe "GBAU123"
    }

    "have a goods location address" in {

      view.getElementById("location-address-label").text() mustBe messages("declaration.summary.locations.goodsLocationAddress")
      view.getElementById("location-address-0").text() mustBe "addressLine"
      view.getElementById("location-address-1").text() mustBe "city"
      view.getElementById("location-address-2").text() mustBe "postCode"
      view.getElementById("location-address-3").text() mustBe "United Kingdom"
    }

    "have office of exit id" in {

      view.getElementById("location-officeOfExit-label").text() mustBe messages("declaration.summary.locations.officeOfExit")
      view.getElementById("location-officeOfExit").text() mustBe "123"
    }

    "have express consignment answer" in {

      view.getElementById("location-expressConsignment-label").text() mustBe messages("declaration.summary.locations.expressConsignment")
      view.getElementById("location-expressConsignment").text() mustBe "12"
    }
  }
}