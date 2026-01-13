package com.reltio.workflow.external.services;

import com.reltio.workflow.external.services.beans.ChangeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    @Test
    public void testIsCreateEntity() {
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setChanges(null);
        assertFalse(Utils.isCreateEntity(changeRequest, "entities/uri"));
    }
}