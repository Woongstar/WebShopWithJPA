package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;


    @Test
    public void join() throws Exception {

        //given
        Member member = new Member();
        member.setName("지웅");

        //when
        Long savedId = memberService.join(member);

        //then
        assertEquals(member, memberRepository.findOne(savedId));

    }


    @Test(expected = IllegalStateException.class)
    public void duplicateTest() {
        Member member1 = new Member();
        member1.setName("WOONGSTAR");

        Member member2 = new Member();
        member2.setName("WOONGSTAR");

        memberService.join(member1);
        memberService.join(member2);

        fail("여기 코드가 실행되면 에러 발생");

    }
}