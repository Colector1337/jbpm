/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jbpm.executor.commands.error;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.jbpm.executor.impl.error.JobExecutionErrorFilter;
import org.jbpm.runtime.manager.impl.jpa.ExecutionErrorInfo;
import org.kie.api.executor.STATUS;

/**
 * Command that will auto acknowledge async jobs errors based on their status:
 * - if job that previously failed is completed it will be eligible for auto ack
 * - if job that previously failed is cancelled it will be eligible for auto ack
 * - if job that previously failed is rescheduled it will be eligible for auto ack
 * 
 * Following parameters are supported by this command:
 * <ul>
 *  <li>EmfName - name of entity manager factory to be used for queries (valid persistence unit name)</li>
 *  <li>SingleRun - indicates if execution should be single run only (true|false)</li>
 *  <li>NextRun - provides next execution time (valid time expression e.g. 1d, 5h, etc)</li>
 * </ul>
 */
public class JobAutoAckErrorCommand extends AutoAckErrorCommand {

    private static final String RULE = "Jobs that previously failed but now are in one of the statuses - queued, completed or cancelled";
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<ExecutionErrorInfo> findErrorsToAck(EntityManager em) {
        String findJobErrorsQuery = "select error from ExecutionErrorInfo error "
                + "where error.type = :type "
                + "and error.acknowledged =:acknowledged "
                + "and error.jobId in (select req.id from RequestInfo req where status in (:status))";
        
        List<ExecutionErrorInfo> errorsToAck = em.createQuery(findJobErrorsQuery)
                .setParameter("type", JobExecutionErrorFilter.TYPE)
                .setParameter("acknowledged", new Short("0"))
                .setParameter("status", Arrays.asList(STATUS.DONE, STATUS.CANCELLED, STATUS.QUEUED))
                .getResultList();

        return errorsToAck;
        
    }

    @Override
    protected String getAckRule() {
        return RULE;
    }

}
