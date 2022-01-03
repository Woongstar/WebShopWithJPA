package jpabook.jpashop;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class MemberRepositoryTest {
        @Autowired MemberRepository memberRepository;

        @Test
        @Transactional
    public void findMember(){
            Member member = new Member();
            member.setUsername("memberA");
            Long savedId = memberRepository.save(member);

            Member findMember = memberRepository.find(savedId);
            Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
            Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());

        }
}