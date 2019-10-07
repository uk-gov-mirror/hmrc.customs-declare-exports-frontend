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

package services

import base.{Injector, MockConnectors, MockExportCacheService}
import com.kenshoo.play.metrics.Metrics
import config.AppConfig
import connectors.CustomsDeclareExportsConnector
import forms.declaration.LegalDeclaration
import metrics.{ExportsMetrics, MetricIdentifiers}
import models.DeclarationStatus
import models.declaration.submissions.{Action, Submission}
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import services.audit.{AuditService, AuditTypes, EventData}
import services.cache.SubmissionBuilder
import uk.gov.hmrc.http.HeaderCarrier
import unit.base.UnitSpec

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubmissionServiceSpec
    extends UnitSpec with MockExportCacheService with MockConnectors with ScalaFutures with OptionValues with Injector
    with SubmissionBuilder {

  private val auditService = mock[AuditService]
  private val connector = mock[CustomsDeclareExportsConnector]
  private val appConfig = instanceOf[AppConfig]
  private val exportMetrics = instanceOf[ExportsMetrics]
  private val hc: HeaderCarrier = mock[HeaderCarrier]
  private val legal = LegalDeclaration("Name", "Role", "email@test.com", confirmation = true)
  private val auditData = Map(
    EventData.EORI.toString -> "eori",
    EventData.LRN.toString -> "123LRN",
    EventData.DUCR.toString -> "ducr",
    EventData.DecType.toString -> "STD",
    EventData.FullName.toString -> legal.fullName,
    EventData.JobRole.toString -> legal.jobRole,
    EventData.Email.toString -> legal.email,
    EventData.Confirmed.toString -> legal.confirmation.toString,
    EventData.SubmissionResult.toString -> "Success"
  )
  private val submissionService = new SubmissionService(appConfig, connector, auditService, exportMetrics)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, auditService)
  }

  "SubmissionService" should {
    val registry = instanceOf[Metrics].defaultRegistry
    val metric = MetricIdentifiers.submissionMetric
    val timerBefore = registry.getTimers.get(exportMetrics.timerName(metric)).getCount
    val counterBefore = registry.getCounters.get(exportMetrics.counterName(metric)).getCount

    "submit to the back end" when {
      "valid declaration" in {
        // Given
        val declaration = aDeclaration(
          withId("id"),
          withStatus(DeclarationStatus.DRAFT),
          withChoice("STD"),
          withConsignmentReferences(ducr = "ducr", lrn = "123LRN")
        )
        val submission = Submission(uuid = "id", eori = "eori", lrn = "lrn", actions = Seq.empty[Action])
        when(connector.submitDeclaration(any[String])(any(), any())).thenReturn(Future.successful(submission))

        // When
        submissionService.submit("eori", declaration, legal)(hc, global).futureValue.value mustBe "123LRN"

        // Then
        verify(connector).submitDeclaration(equalTo("id"))(equalTo(hc), any())
        verify(auditService).auditAllPagesUserInput(equalTo(AuditTypes.SubmissionPayload), equalTo(declaration))(
          equalTo(hc)
        )
        verify(auditService).audit(equalTo(AuditTypes.Submission), equalTo[Map[String, String]](auditData))(equalTo(hc))
        registry.getTimers.get(exportMetrics.timerName(metric)).getCount mustBe >(timerBefore)
        registry.getCounters.get(exportMetrics.counterName(metric)).getCount mustBe >(counterBefore)
      }
    }
  }
}
