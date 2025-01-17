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

import forms.declaration.Seal
import org.scalatest.{MustMatchers, WordSpec}

class TransportInformationSpec extends WordSpec with MustMatchers {

  "TransportData" should {

    "add container to empty collection" in {
      val container1 = Container("cont1", Seq.empty)

      val data = TransportInformation(containers = Seq.empty)

      data.addOrUpdateContainer(container1) mustBe Seq(container1)
    }

    "add new container to collection" in {
      val container1 = Container("cont1", Seq.empty)
      val container2 = Container("cont2", Seq(Seal("seal1")))
      val containerNew = Container("contNew", Seq.empty)

      val data = TransportInformation(containers = Seq(container1, container2))

      data.addOrUpdateContainer(containerNew) mustBe Seq(container1, container2, containerNew)
    }

    "replace container in collection" in {
      val container1 = Container("cont1", Seq.empty)
      val container2 = Container("cont2", Seq(Seal("seal1")))
      val container1Updated = Container("cont1", Seq.empty)

      val data = TransportInformation(containers = Seq(container1, container2))

      data.addOrUpdateContainer(container1Updated) mustBe Seq(container1Updated, container2)
    }
  }
}
