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

package forms.declaration

import play.api.libs.json.{JsObject, JsValue}

object CarrierDetailsSpec {
  import forms.declaration.EntityDetailsSpec._

  val correctCarrierDetailsJSON: JsValue = JsObject(Map("details" -> correctEntityDetailsJSON))
  val correctCarrierDetailsEORIOnlyJSON: JsValue = JsObject(Map("details" -> correctEntityDetailsEORIOnlyJSON))
  val correctCarrierDetailsAddressOnlyJSON: JsValue = JsObject(Map("details" -> correctEntityDetailsAddressOnlyJSON))
  val emptyCarrierDetailsJSON: JsValue = JsObject(Map("details" -> emptyEntityDetailsJSON))
}
