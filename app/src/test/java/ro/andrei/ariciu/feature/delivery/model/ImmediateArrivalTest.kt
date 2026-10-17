package ro.andrei.ariciu.feature.delivery.model

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation

class ImmediateArrivalTest {
    private val area = SelectedLocation(45.0,25.0,200)
    private val fix = ArrivalLocationFix(45.0,25.0,10.0,1_000,7)
    @Test fun freshAccurateFixMustFitWithUncertaintyInsideRadius() {
        assertEquals(ArrivalCheck.INSIDE, assessArrival(area, fix, 2_000, 7))
        assertEquals(ArrivalCheck.UNCERTAIN, assessArrival(area, fix.copy(accuracyMeters = 100.0),2_000,7))
        assertEquals(ArrivalCheck.OUTSIDE, assessArrival(area, fix.copy(latitude = 45.01),2_000,7))
        assertEquals(ArrivalCheck.UNCERTAIN, assessArrival(area, fix.copy(latitude = 45.0016, accuracyMeters = 20.0),2_000,7))
    }
    @Test fun staleFutureWrongBootMissingAndInvalidAccuracyCannotTrigger() {
        for (bad in listOf(fix.copy(elapsedMs = -1), fix.copy(elapsedMs = 3_000), fix.copy(boot = 6),
            fix.copy(accuracyMeters = Double.NaN), fix.copy(accuracyMeters = -1.0), fix.copy(latitude = Double.NaN))) {
            assertNotEquals(ArrivalCheck.INSIDE, assessArrival(area,bad,2_000,7))
        }
        assertEquals(ArrivalCheck.STALE,assessArrival(area,fix,ARRIVAL_MAX_AGE_MS + 1_001,7))
    }
}
