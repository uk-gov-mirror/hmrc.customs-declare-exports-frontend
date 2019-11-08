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

package forms.declaration

import base.TestHelper
import forms.declaration.TransportCodes._
import helpers.views.declaration.WarehouseIdentificationMessages
import play.api.libs.json.{JsObject, JsString, JsValue}
import unit.base.UnitSpec

class WarehouseDetailsSpec extends UnitSpec with WarehouseIdentificationMessages {

  import WarehouseDetails._

  "Warehouse Identification Form" should {
    "validate identification type" in {
      val incorrectWarehouseDetails: JsValue =
        JsObject(Map("identificationType" -> JsString(TestHelper.createRandomAlphanumericString(2))))

      form().bind(incorrectWarehouseDetails).errors.map(_.message) must contain(identificationTypeError)
    }

    "validate identification type present and number missing" in {

      val incorrectWarehouseDetails: JsValue =
        JsObject(Map("identificationType" -> JsString(WarehouseDetails.IdentifierType.PUBLIC_CUSTOMS_1), "identificationNumber" -> JsString("")))

      form().bind(incorrectWarehouseDetails).errors.map(_.message) must contain(identificationTypeNoNumber)
    }

    "validate identification number present and type missing" in {

      val incorrectWarehouseDetails: JsValue =
        JsObject(Map("identificationType" -> JsString(""), "identificationNumber" -> JsString(TestHelper.createRandomAlphanumericString(2))))

      form().bind(incorrectWarehouseDetails).errors.map(_.message) must contain(identificationNumberNoType)
    }

    "validate identification number - more than 35 characters" in {
      val incorrectWarehouseDetails: JsValue =
        JsObject(Map("identificationNumber" -> JsString(TestHelper.createRandomAlphanumericString(36))))

      form().bind(incorrectWarehouseDetails).errors.map(_.message) must contain(identificationNumberError)
    }

    "validate supervising customs office - invalid" in {

      val incorrectWarehouseOffice: JsValue =
        JsObject(Map("supervisingCustomsOffice" -> JsString("SOMEWRONGCODE")))

      form().bind(incorrectWarehouseOffice).errors.map(_.message) must contain(supervisingCustomsOfficeError)
    }

    "validate inland mode transport code - wrong choice" in {

      val incorrectTransportCode: JsValue =
        JsObject(Map("inlandModeOfTransportCode" -> JsString("Incorrect more")))

      form().bind(incorrectTransportCode).errors.map(_.message) must contain(inlandTransportModeError)
    }

  }
}

object WarehouseDetailsSpec {
  val correctWarehouseDetails =
    WarehouseDetails(Some(office), Some(warehouseTypeCode), Some(warehouseId), Some(inlandModeOfTransportCode))
  val emptyWarehouseDetails = WarehouseDetails(None, None, None, None)
  val correctWarehouseDetailsJSON: JsValue =
    JsObject(
      Map(
        "supervisingCustomsOffice" -> JsString("12345678"),
        "identificationType" -> JsString(warehouseTypeCode),
        "identificationNumber" -> JsString(warehouseId),
        "inlandModeOfTransportCode" -> JsString(Rail)
      )
    )
  val emptyWarehouseDetailsJSON: JsValue =
    JsObject(
      Map(
        "supervisingCustomsOffice" -> JsString(""),
        "identificationType" -> JsString(""),
        "identificationNumber" -> JsString(""),
        "inlandModeOfTransportCode" -> JsString("")
      )
    )
  private val warehouseTypeCode = "R"
  private val warehouseId = "1234567GB"
  private val office = "Office"
  private val inlandModeOfTransportCode = Maritime
}