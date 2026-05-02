package com.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.item.Electronics;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * AuctionTest - Kiểm thử logic cốt lõi của Auction.
 *
 * Covers:
 *  - Vòng đời trạng thái: OPEN → RUNNING → FINISHED / CANCELED / PAID
 *  - Đặt giá hợp lệ và không hợp lệ
 *  - Kiểm tra người dẫn đầu sau mỗi bid
 *  - Xử lý ngoại lệ AuctionClosedException, InvalidBidException
 *  - isExpired()
 */
@DisplayName("Auction - Logic đấu giá cốt lõi")
class AuctionTest {

    private Auction auction;
    private Bidder  bidder1;
    private Bidder  bidder2;
    private Seller  seller;

    @BeforeEach
    void setUp() {
        seller  = new Seller("seller-1", "seller1", "pass", "seller@test.com");
        bidder1 = new Bidder("bidder-1", "alice",   "pass", "alice@test.com");
        bidder2 = new Bidder("bidder-2", "bob",     "pass", "bob@test.com");

        Electronics phone = new Electronics();
        phone.setName("iPhone 15");
        phone.setDescription("Brand new");
        phone.setStartingPrice(10_000_000);
        phone.setSellerId(seller.getId());

        auction = new Auction(
            "auction-test-1",
            phone,
            seller,
            10_000_000,
            LocalDateTime.now().minusMinutes(1),   // đã bắt đầu
            LocalDateTime.now().plusHours(1)        // chưa kết thúc
        );
    }

    // ── Trạng thái ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Trạng thái ban đầu phải là OPEN")
    void initialStatusIsOpen() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    @DisplayName("start() chuyển OPEN → RUNNING")
    void startChangesStatusToRunning() {
        auction.start();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("start() từ trạng thái RUNNING phải ném IllegalStateException")
    void startFromRunningThrows() {
        auction.start();
        assertThrows(IllegalStateException.class, auction::start);
    }

    @Test
    @DisplayName("finish() khi có người thắng → FINISHED")
    void finishWithWinnerIsFinished() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 11_000_000);
        auction.finish();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    @DisplayName("finish() khi không có bid nào → CANCELED")
    void finishWithoutBidIsCanceled() {
        auction.start();
        auction.finish();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("markPaid() sau FINISHED → PAID")
    void markPaidAfterFinished() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 11_000_000);
        auction.finish();
        auction.markPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    @DisplayName("cancel() chuyển về CANCELED từ bất kỳ trạng thái nào")
    void cancelSetsCanceled() {
        auction.start();
        auction.cancel();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    // ── Đặt giá hợp lệ ────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid hợp lệ cập nhật currentPrice và currentWinnerId")
    void validBidUpdatesState() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 12_000_000);

        assertEquals(12_000_000, auction.getCurrentPrice(), 0.01);
        assertEquals(bidder1.getId(), auction.getCurrentWinnerId());
    }

    @Test
    @DisplayName("Bid liên tiếp: người bid cao hơn thành người dẫn đầu")
    void consecutiveBidsUpdateLeader() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 11_000_000);
        auction.placeBid(bidder2.getId(), 13_000_000);

        assertEquals(13_000_000, auction.getCurrentPrice(), 0.01);
        assertEquals(bidder2.getId(), auction.getCurrentWinnerId());
    }

    @Test
    @DisplayName("placeBid() trả về true khi thành công")
    void placeBidReturnsTrue() throws Exception {
        auction.start();
        boolean result = auction.placeBid(bidder1.getId(), 11_000_000);
        assertTrue(result);
    }

    // ── Đặt giá không hợp lệ ──────────────────────────────────────────────

    @Test
    @DisplayName("Bid thấp hơn currentPrice ném InvalidBidException")
    void bidLowerThanCurrentPriceThrows() {
        auction.start();
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), 9_000_000));
    }

    @Test
    @DisplayName("Bid bằng currentPrice ném InvalidBidException")
    void bidEqualCurrentPriceThrows() {
        auction.start();
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), 10_000_000));
    }

    @Test
    @DisplayName("Người đang dẫn đầu đặt lại ném InvalidBidException")
    void leadingBidderBidAgainThrows() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 11_000_000);
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), 12_000_000));
    }

    @Test
    @DisplayName("Bid khi phiên OPEN (chưa start) ném AuctionClosedException")
    void bidOnOpenAuctionThrows() {
        // status = OPEN, chưa gọi start()
        assertThrows(AuctionClosedException.class,
            () -> auction.placeBid(bidder1.getId(), 11_000_000));
    }

    @Test
    @DisplayName("Bid sau khi FINISHED ném AuctionClosedException")
    void bidOnFinishedAuctionThrows() throws Exception {
        auction.start();
        auction.placeBid(bidder1.getId(), 11_000_000);
        auction.finish();
        assertThrows(AuctionClosedException.class,
            () -> auction.placeBid(bidder2.getId(), 12_000_000));
    }

    @Test
    @DisplayName("Bid sau khi CANCELED ném AuctionClosedException")
    void bidOnCanceledAuctionThrows() {
        auction.start();
        auction.cancel();
        assertThrows(AuctionClosedException.class,
            () -> auction.placeBid(bidder1.getId(), 11_000_000));
    }

    // ── isExpired ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isExpired() = false khi chưa hết giờ")
    void isExpiredFalseWhenNotExpired() {
        assertFalse(auction.isExpired());
    }

    @Test
    @DisplayName("isExpired() = true khi đã quá giờ")
    void isExpiredTrueWhenPastEndTime() {
        Auction expired = new Auction(
            "expired-auction",
            auction.getItem(),
            seller,
            10_000_000,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusSeconds(1)  // đã hết giờ
        );
        assertTrue(expired.isExpired());
    }
}
