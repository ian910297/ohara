import React from 'react';
import { shallow } from 'enzyme';

import AppWrapper from '../AppWrapper';

const children = () => <p>children</p>;

const props = {
  title: 'test',
  children: children(),
};

describe('<AppWrapper />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<AppWrapper {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
  });

  it('renders <H2 />', () => {
    expect(
      wrapper
        .find('H2')
        .children()
        .text(),
    ).toBe(props.title);
  });

  it('renders children', () => {
    expect(wrapper.find('p').length).toBe(1);
    expect(
      wrapper
        .find('p')
        .children()
        .text(),
    ).toBe('children');
  });
});
