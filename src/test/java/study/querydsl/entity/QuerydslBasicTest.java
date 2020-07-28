package study.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

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

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl2() {
       Member findMember = queryFactory
               .select(member)
               .from(member)
               .where(member.username.eq("member1"))
               .fetchOne();

       assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     *     member.username.eq("member1") // username = 'member1'
     *     member.username.ne("member1") //username != 'member1'
     *     member.username.eq("member1").not() // username != 'member1'
     *     member.username.isNotNull() //이름이 is not null
     *     member.age.in(10, 20) // age in (10,20)
     *     member.age.notIn(10, 20) // age not in (10, 20)
     *     member.age.between(10,30) //between 10, 30
     *     member.age.goe(30) // age >= 30
     *     member.age.gt(30) // age > 30
     *     member.age.loe(30) // age <= 30
     *     member.age.lt(30) // age < 30
     *     member.username.like("member%") //like 검색
     *     member.username.contains("member") // like ‘%member%’ 검색
     *     member.username.startsWith("member") //like ‘member%’ 검색
     */

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

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
        assertThat(findMember).isNotNull();
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

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징 1
     * 조회 건수 제한
     */
    @Test
    public void paging1 () {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2) // 2개만 가져오는 limit
                .fetch(); // count쿼리가 날려지지 않는다.

        assertThat(result.size()).isEqualTo(2);
    }
    /**
     * 페이징 2
     * 전체 조회수가 필요하다면 ?
     * 주의: count 쿼리가 실행되니 성능상 주의!
     * 참고: 실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만,
     * count 쿼리는 조인이 필요 없는 경우도 있다. 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두
     * 조인을 해버리기 때문에 성능이 안나올 수 있다. count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면,
     * count 전용 쿼리를 별도로 작성해야 한다.
     */
    @Test
    public void paging2 () {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults(); // fetchResults 는 count쿼리가 날려진다. 즉, getTotal() 메소드를 사용할 수 있다.

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * [집합] JPQL
     * select
     *  COUNT(m), //회원수
     *  SUM(m.age), //나이 합
     *  AVG(m.age), //평균 나이
     *  MAX(m.age), //최대 나이
     *  MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        // tuple = [4, 100, 25.0, 40, 10]

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    /**
     * GroupBy 사용
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     * groupBy , 그룹화된 결과를 제한하려면 having
     *  …
     *  .groupBy(item.price)
     *  .having(item.price.gt(1000))
     *  …
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 기본 조인
     * 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭으로 사용할 Q타입을 지정하면 된다.
     * join(조인대상, 별칭으로 사용할 Q타입)
     *
     * join() , innerJoin() : 내부 조인(inner join)
     * leftJoin() : left 외부 조인(left outer join)
     * rightJoin() : right 외부 조인(right outer join)
     * JPQL의 on 과 성능 최적화를 위한 fetch 조인 제공 다음 on 절에서 설명
     *
     * 테스트케이스 예시 ) 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        QMember member = QMember.member;
        QTeam team = QTeam.team;

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

}
