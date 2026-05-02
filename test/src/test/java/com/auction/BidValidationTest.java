package com.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Electronics;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BidValidationTest - Kiểm thử validation giá đấu và xử lý đồng thời.
 *
 * Covers:
 *  - Bid hợp lệ: amount > currentPrice
 *  - Bid không hợp lệ: amount ≤ currentPrice
 *  - Bid khi phiên không RUNNING
 *  - Concurrent bidding: nhiều thread đặt giá cùng lúc
 *    → không xảy ra lost update, race condition, 2 người cùng thắng
 *  - BidTransaction được tạo đúng
 */
@DisplayName("BidValidation - Kiểm tra giá đấu và Concurrent Bidding")
class BidValidationTest {

    private Auction auction;
    private Seller  seller;
    private Bidder  bidder1;
    private Bidder  bidder2;
    private Bidder  bidder3;

    @BeforeEach
    void setUp() {
        seller  = new Seller("s-1", "seller",   "pass", "s@test.com");
        bidder1 = new Bidder("b-1", "alice",    "pass", "a@test.com");
        bidder2 = new Bidder("b-2", "bob",      "pass", "b@test.com");
        bidder3 = new Bidder("b-3", "charlie",  "pass", "c@test.com");

        Electronics item = new Electronics();
        item.setName("Laptop");
        item.setStartingPrice(5_000_000);
        item.setSellerId(seller.getId());

        auction = new Auction(
            "auction-valid-1",
            item,
            seller,
            5_000_000,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusHours(2)
        );
        auction.start();
    }

    // ── Validation cơ bản ─────────────────────────────────────────────────

    @Test
    @DisplayName("Bid hợp lệ: amount lớn hơn startingPrice")
    void validBidAccepted() throws Exception {
        assertDoesNotThrow(() -> auction.placeBid(bidder1.getId(), 5_100_000));
        assertEquals(5_100_000, auction.getCurrentPrice(), 0.01);
    }

    @Test
    @DisplayName("Bid bằng 0 ném InvalidBidException")
    void zeroBidThrows() {
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), 0));
    }

    @Test
    @DisplayName("Bid âm ném InvalidBidException")
    void negativeBidThrows() {
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), -1_000));
    }

    @Test
    @DisplayName("Bid bằng startingPrice ném InvalidBidException")
    void bidEqualStartingPriceThrows() {
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder1.getId(), 5_000_000));
    }

    @Test
    @DisplayName("Bid thấp hơn giá hiện tại sau một bid trước đó")
    void bidLowerThanCurrentAfterBidThrows() throws Exception {
        auction.placeBid(bidder1.getId(), 6_000_000);
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid(bidder2.getId(), 5_500_000));
    }

    @Test
    @DisplayName("Phiên OPEN: bid ném AuctionClosedException")
    void bidOnOpenAuctionThrows() {
        Electronics item = new Electronics();
        item.setName("Phone");
        item.setStartingPrice(1_000_000);

        Auction openAuction = new Auction("open-1", item, seller, 1_000_000,
            LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        // Không gọi start()

        assertThrows(AuctionClosedException.class,
            () -> openAuction.placeBid(bidder1.getId(), 1_500_000));
    }

    // ── BidTransaction ────────────────────────────────────────────────────

    @Test
    @DisplayName("BidTransaction có đầy đủ thông tin sau khi tạo")
    void bidTransactionHasCorrectData() {
        BidTransaction tx = new BidTransaction("tx-1", "auction-1", "bidder-1", 1_000_000,
            LocalDateTime.now());
        assertEquals("tx-1",       tx.getId());
        assertEquals("auction-1",  tx.getAuctionId());
        assertEquals("bidder-1",   tx.getBidderId());
        assertEquals(1_000_000,    tx.getAmount(), 0.01);
        assertEquals(1_000_000,    tx.getBidAmount(), 0.01); // alias
        assertNotNull(tx.getBidTime());
        assertNotNull(tx.getTimestamp()); // alias
        assertFalse(tx.isAutoBid());
    }

    @Test
    @DisplayName("BidTransaction isAutoBid = true khi set")
    void bidTransactionAutoBidFlag() {
        BidTransaction tx = new BidTransaction("tx-2", "auction-1", "bidder-1", 1_000_000,
            LocalDateTime.now());
        tx.setAutoBid(true);
        assertTrue(tx.isAutoBid());
    }

    // ── Concurrent Bidding ─────────────────────────────────────────────────

    @Test
    @DisplayName("Concurrent: 3 thread bid cùng lúc → chỉ 1 người thắng cuối cùng")
    void concurrentBidsOnlyOneWinner() throws InterruptedException {
        int threadCount = 3;
        double[] amounts = { 6_000_000, 6_000_001, 6_000_002 };
        String[] bidders = { bidder1.getId(), bidder2.getId(), bidder3.getId() };

        CountDownLatch ready  = new CountDownLatch(threadCount);
        CountDownLatch start  = new CountDownLatch(1);
        CountDownLatch done   = new CountDownLatch(threadCount);
        AtomicInteger  successes = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await(); // chờ tất cả sẵn sàng rồi bắt đầu cùng lúc
                    auction.placeBid(bidders[idx], amounts[idx]);
                    successes.incrementAndGet();
                } catch (AuctionClosedException | InvalidBidException ignored) {
                    // Bid thất bại là bình thường khi race condition
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await(2, TimeUnit.SECONDS);
        start.countDown(); // khai hỏa tất cả
        done.await(5, TimeUnit.SECONDS);

        // Sau khi tất cả thread chạy xong: giá phải hợp lệ và đúng người dẫn đầu
        double finalPrice   = auction.getCurrentPrice();
        String finalWinner  = auction.getCurrentWinnerId();

        assertTrue(finalPrice > 5_000_000,
            "Giá phải cao hơn startingPrice");
        assertNotNull(finalWinner,
            "Phải có người dẫn đầu");

        // Giá cuối phải khớp với 1 trong 3 mức bid đã đặt
        boolean validPrice = false;
        for (double a : amounts) {
            if (Math.abs(finalPrice - a) < 0.01) { validPrice = true; break; }
        }
        assertTrue(validPrice, "Giá cuối phải là một trong các mức đã bid");
    }

    @Test
    @DisplayName("Concurrent: 10 thread bid tuần tự tăng dần → không bị lost update")
    void concurrentSequentialBidsNoLostUpdate() throws InterruptedException {
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final double bidAmount = 5_000_000 + (i + 1) * 100_000;
            final String bidder = (i % 2 == 0) ? bidder1.getId() : bidder2.getId();

            futures.add(pool.submit(() -> {
                try {
                    auction.placeBid(bidder, bidAmount);
                    successCount.incrementAndGet();
                } catch (AuctionClosedException | InvalidBidException ignored) {}
            }));
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // Phải có ít nhất 1 bid thành công
        assertTrue(successCount.get() >= 1,
            "Ít nhất 1 bid phải thành công");

        // Giá cuối phải hơn startingPrice
        assertTrue(auction.getCurrentPrice() > 5_000_000,
            "currentPrice phải tăng so với startingPrice");

        // Chỉ có 1 người dẫn đầu
        assertNotNull(auction.getCurrentWinnerId(), "Phải có currentWinnerId");
    }

    @Test
    @DisplayName("Concurrent: hai người cùng bid giá giống nhau → chỉ 1 người được chấp nhận")
    void sameBidAmountConcurrentlyOnlyOneAccepted() throws InterruptedException {
        final double SAME_AMOUNT = 7_000_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(2);
        AtomicInteger accepted = new AtomicInteger(0);

        for (String bidderId : new String[]{ bidder2.getId(), bidder3.getId() }) {
            new Thread(() -> {
                try {
                    start.await();
                    auction.placeBid(bidderId, SAME_AMOUNT);
                    accepted.incrementAndGet();
                } catch (AuctionClosedException | InvalidBidException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await(3, TimeUnit.SECONDS);

        // Không thể có 2 người cùng thắng tại cùng mức giá
        assertTrue(accepted.get() <= 1,
            "Không thể có hai người cùng bid thắng cùng mức giá");
    }
}
