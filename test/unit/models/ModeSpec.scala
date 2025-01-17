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

package unit.models

import models.Mode.{Amend, Change, ChangeAmend, Draft, ErrorFix, Normal}
import unit.base.UnitSpec

class ModeSpec extends UnitSpec {

  "Normal mode" must {
    "be same after submitting form" in {
      Normal.next mustBe Normal
    }
  }

  "Change mode" must {
    "become Normal after submitting form" in {
      Change.next mustBe Normal
    }
  }

  "Amend mode" must {
    "be same after submitting form" in {
      Amend.next mustBe Amend
    }
  }

  "Change-Amend" must {
    "become Amend after submitting form" in {
      ChangeAmend.next mustBe Amend
    }
  }

  "Draft" must {
    "be same after submitting page" in {
      Draft.next mustBe Normal
    }
  }

  "Error-Fix" must {
    "be same after submitting page" in {
      ErrorFix.next mustBe ErrorFix
    }
  }
}
