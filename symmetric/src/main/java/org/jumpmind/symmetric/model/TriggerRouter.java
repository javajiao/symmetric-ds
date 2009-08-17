/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>
 *               Eric Long <erilong@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.jumpmind.symmetric.model;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;

/**
 * Defines the trigger via which a table will be synchronized.
 */
public class TriggerRouter {

    static final Log logger = LogFactory.getLog(TriggerRouter.class);

    private static final long serialVersionUID = 8947288471097851573L;

    /**
     * This is the order in which the definitions will be processed.
     */
    private int initialLoadOrder;

    private Trigger trigger;

    private Router router;

    private Date createdOn;

    private Date lastModifiedTime;

    private String updatedBy;

    public TriggerRouter() {
        trigger = new Trigger();
        router = new Router();
        createdOn = new Date();
        lastModifiedTime = new Date();
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public void setLastModifiedTime(Date lastModifiedOn) {
        this.lastModifiedTime = lastModifiedOn;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * When dealing with columns, always use this method to order the columns so that the primary keys are first.
     */
    public Column[] orderColumnsForTable(Table table) {
        return trigger.orderColumnsForTable(table);
    }

    public int getInitialLoadOrder() {
        return initialLoadOrder;
    }

    public void setInitialLoadOrder(int order) {
        this.initialLoadOrder = order;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public Router getRouter() {
        return router;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Trigger getTrigger() {
        return trigger;
    }
}
