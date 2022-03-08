package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderServiceTest {
    @Autowired
    EntityManager em;

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Test
    public void orderTest() {

        Member member = createMember();

        Book book = createBook("JPA BOOK", 100000, 10);

        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("상품 주문시 상태는 ORDER",OrderStatus.ORDER,getOrder.getStatus());
        assertEquals("주문 상품 수량 확인",1,getOrder.getOrderItems().size());
        assertEquals("상품 가격은 가격 * 수량이다.",100000*orderCount,getOrder.getTotalPrice());
        assertEquals("주문 시 재고가 줄어야한다",8,book.getStockQuantity());

    }

    @Test(expected = NotEnoughStockException.class)
    public void outOfQuantityTest() throws Exception{

        Member member = createMember();
        Item item = createBook("JPABOOK", 10000, 20);
        int orderCount = 21;
        orderService.order(member.getId(), item.getId(), orderCount);

        fail("재고부족시 에러 떠야함");


    }

    @Test
    public void orderCanceled() {
        Member member = createMember();
        Item item = createBook("WOONGSTAR BOOK",20000, 25);
        int orderCount = 5;
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        orderService.cancelOrder(orderId);

        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("주문 취소시 상태는 CANCEL이다",OrderStatus.CANCEL,getOrder.getStatus());
        assertEquals("재고가 원상복구 되어야 한다.",item.getStockQuantity(),25);
    }


    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        Item item = new Book();
        member.setAddress(new Address("서울","강남","123123"));
        em.persist(member);
        return member;
    }

}