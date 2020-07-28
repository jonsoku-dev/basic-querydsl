package study.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
@Commit
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    // 테스트 코드가 실행되기 전에 실행 되는 부분
    // 여기에서는 팀과 멤버를 DB에 넣는다.
    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl2() {
       Member findMember = queryFactory
               .select(member)
               .from(member)
               .where(member.username.eq("member1"))
               .fetchOne();

       Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // member.username.eq("member1") // username = 'member1'
    //member.username.ne("member1") //username != 'member1'
    //member.username.eq("member1").not() // username != 'member1'
    //member.username.isNotNull() //이름이 is not null
    //member.age.in(10, 20) // age in (10,20)
    //member.age.notIn(10, 20) // age not in (10, 20)
    //member.age.between(10,30) //between 10, 30
    //member.age.goe(30) // age >= 30
    //member.age.gt(30) // age > 30
    //member.age.loe(30) // age <= 30
    //member.age.lt(30) // age < 30
    //member.username.like("member%") //like 검색
    //member.username.contains("member") // like ‘%member%’ 검색
    //member.username.startsWith("member") //like ‘member%’ 검색

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                // 검색 조건은 and, or를 메서드 체인으로 연결 할 수 있다.
                // and의 경우 ,으로 구분할 수 있다.
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
        Assertions.assertThat(findMember.getAge()).isEqualTo(10);
        Assertions.assertThat(findMember).isNotNull();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단, 2에서 회원 이름이 없으면 마지막 출력 (nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // null이면 제일 마지막으로 보낸다.
                .fetch();

        Member member5 = result.get(0); // 이름 오른차순 5
        Member member6 = result.get(1); // 이름 오츰차순 6
        Member memberNull = result.get(2); // null이라서 마지막으로 갔는지 확인

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징
     */
    @Test
    public void paging1 () {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2) // 2개만 가져오는 limit
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }
}
