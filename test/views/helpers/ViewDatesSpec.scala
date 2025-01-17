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

package views.helpers

import java.time.LocalDateTime

import org.scalatest.{MustMatchers, WordSpec}

class ViewDatesSpec extends WordSpec with MustMatchers {

  "ViewDates" should {

    "format date at time correctly" in {

      val date = LocalDateTime.of(2019, 8, 20, 13, 55, 15)
      ViewDates.formatDateAtTime(date) must equal("20 August 2019 at 1:55pm")
    }
  }
}
