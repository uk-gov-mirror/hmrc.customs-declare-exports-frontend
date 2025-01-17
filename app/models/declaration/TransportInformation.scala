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

package models.declaration

import forms.declaration.TransportPayment
import play.api.libs.json.Json

case class TransportInformation(transportPayment: Option[TransportPayment] = None, containers: Seq[Container] = Seq.empty) {

  private def hasContainer(id: String) = containers.exists(_.id == id)

  def addOrUpdateContainer(updatedContainer: Container): Seq[Container] =
    if (containers.isEmpty) {
      Seq(updatedContainer)
    } else if (hasContainer(updatedContainer.id)) {
      containers.map {
        case container if updatedContainer.id == container.id => updatedContainer
        case otherContainer                                   => otherContainer
      }
    } else {
      containers :+ updatedContainer
    }
}

object TransportInformation {
  implicit val format = Json.format[TransportInformation]
}
