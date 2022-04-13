package com.mengxinya.it;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockUtilTests {

    @Test
    public void testMock1() {
        IServiceLogic logic = MockUtil.mock(IServiceLogic.class);
        Assertions.assertEquals(0, logic.getAge());
        Assertions.assertNull(logic.getByApi());
        Assertions.assertNull(logic.getName());
    }

    @Test
    public void testMock2() {
        IServiceLogic logic = MockUtil.mock(IServiceLogic.class, ServiceLogicMock.class);
        Assertions.assertEquals(0, logic.getAge());
        Assertions.assertNull(logic.getByApi());
        Assertions.assertEquals("Mock_Name", logic.getName());
    }

    @Test
    public void testMock3() {
        IServiceLogic logic = MockUtil.mock(ServiceLogicImpl.class, ServiceLogicMock3.class);
        Assertions.assertEquals(20, logic.getAge());
        Assertions.assertEquals(new UserInfo("MockName", 30), logic.getByApi());
    }

    // TODO 有参数的构造方法出现错误
    @Test
    public void testMock4() {
        IServiceLogic logic = MockUtil.mock(ServiceLogicImpl.class, ServiceLogicMock3.class, (RemoteApi) () -> new UserInfo("Lily", 20));
        Assertions.assertEquals(20, logic.getAge());
        Assertions.assertEquals(new UserInfo("MockName", 30), logic.getByApi());
    }

    public static class ServiceLogicMock {
        public String getName() {
            return "Mock_Name";
        }
    }

    public static class ServiceLogicMock3 {
        private RemoteApi api = () -> new UserInfo("MockName", 30);
    }

    public interface IServiceLogic {
        UserInfo getByApi();
        int getAge();
        String getName();
    }

    public static class ServiceLogicImpl implements IServiceLogic {
        private RemoteApi api;
        public ServiceLogicImpl() {}
        public ServiceLogicImpl(RemoteApi api) {
            this.api = api;
        }

        @Override
        public UserInfo getByApi() {
            return api.getUserInfo();
        }

        @Override
        public int getAge() {
            return 20;
        }

        @Override
        public String getName() {
            return "Lily";
        }
    }

    public interface RemoteApi {
        UserInfo getUserInfo();
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserInfo {
        private String name;
        private int age;
    }
}
